package com.lunchy

import com.lunchy.domain.Budget
import com.lunchy.domain.EngineStep
import com.lunchy.domain.RadiusPolicy
import com.lunchy.domain.SearchFilters
import com.lunchy.domain.SearchPage
import com.lunchy.domain.SuggestionSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SuggestionSessionTest {
    private val filters = SearchFilters(radiusKm = 5.0, minRating = null, budget = Budget.ANY, keyword = "")

    @Test
    fun searchAgainSkipsPlacesAlreadyShown() {
        val session = SuggestionSession(random = Random(1))
        val places = (1..10).map { samplePlace("p$it", kmNorth = 0.4) }
        val firstFetch = session.next(filters, CANBERRA)
        assertTrue(firstFetch is EngineStep.Fetch)
        val first = session.commit(SearchPage(places, null), firstFetch as EngineStep.Fetch) as EngineStep.Done
        assertEquals(5, first.outcome.places.size)
        val shown = first.outcome.places.map { it.id }.toSet()

        val second = session.next(filters, CANBERRA)
        assertTrue(second is EngineStep.Done)
        val more = (second as EngineStep.Done).outcome.places
        assertEquals(5, more.size)
        assertTrue(more.map { it.id }.none { it in shown })
        assertEquals(10, session.shownIds().size)
    }

    @Test
    fun pagesBeforeWidening() {
        val session = SuggestionSession(random = Random(2))
        val first = (1..3).map { samplePlace("a$it", kmNorth = 0.2) }
        val rest = (1..6).map { samplePlace("b$it", kmNorth = 0.5) }
        val fetch = session.next(filters, CANBERRA) as EngineStep.Fetch
        val next = session.commit(SearchPage(first, "page-2"), fetch)
        assertTrue(next is EngineStep.Fetch)
        val secondFetch = next as EngineStep.Fetch
        assertEquals("page-2", secondFetch.pageToken)
        assertEquals(5.0, secondFetch.radiusKm, 0.01)
        val done = session.commit(SearchPage(rest, null), secondFetch) as EngineStep.Done
        assertEquals(5, done.outcome.places.size)
        assertTrue(done.outcome.places.none { it.openNow == false })
    }

    @Test
    fun widensWhenThePageCannotFillFive() {
        val session = SuggestionSession(random = Random(4))
        val outside = listOf(samplePlace("far", kmNorth = 9.0))
        val fetch = session.next(filters, CANBERRA) as EngineStep.Fetch
        val widened = session.commit(SearchPage(outside, null), fetch)
        assertTrue(widened is EngineStep.Fetch)
        assertTrue((widened as EngineStep.Fetch).radiusKm > filters.radiusKm)
        assertEquals("DISTANCE", widened.rankPreference)
        assertNull(widened.pageToken)
    }

    @Test
    fun aFailedFetchDoesNotWiden() {
        val session = SuggestionSession(random = Random(0))
        val first = session.next(filters, CANBERRA) as EngineStep.Fetch
        val second = session.next(filters, CANBERRA) as EngineStep.Fetch
        assertEquals(first.radiusKm, second.radiusKm, 0.001)
        assertNull(second.pageToken)
        assertEquals("RELEVANCE", second.rankPreference)
    }

    @Test
    fun exclusionSurvivesAFilterChange() {
        val session = SuggestionSession(random = Random(7))
        val places = (1..12).map { samplePlace("p$it", kmNorth = 0.3) }
        val fetch = session.next(filters, CANBERRA) as EngineStep.Fetch
        val done = session.commit(SearchPage(places, null), fetch) as EngineStep.Done
        val shown = done.outcome.places.map { it.id }.toSet()
        val changed = filters.copy(keyword = "ramen", radiusKm = 5.0)
        val again = session.next(changed, CANBERRA)
        assertTrue(again is EngineStep.Fetch)
        val second = session.commit(SearchPage(places, null), again as EngineStep.Fetch) as EngineStep.Done
        assertEquals(5, second.outcome.places.size)
        assertTrue(second.outcome.places.none { it.id in shown })
    }

    @Test
    fun tellsTheUserWhenNothingNewIsLeft() {
        val tight = SearchFilters(radiusKm = 25.0, minRating = null, budget = Budget.ANY, keyword = "")
        val session = SuggestionSession(random = Random(1))
        val fetch = session.next(tight, CANBERRA) as EngineStep.Fetch
        val done = session.commit(
            SearchPage(listOf(samplePlace("only", kmNorth = 0.2)), null),
            fetch,
        ) as EngineStep.Done
        assertEquals(1, done.outcome.places.size)
        assertTrue(done.outcome.exhausted)
        assertFalse(done.outcome.canSearchAgain)
        val again = session.next(tight, CANBERRA) as EngineStep.Done
        assertTrue(again.outcome.places.isEmpty())
        assertTrue(again.outcome.exhausted)
    }

    @Test
    fun closedPlacesNeverFillTheFive() {
        val session = SuggestionSession(random = Random(1))
        val open = (1..3).map { samplePlace("o$it", open = true) }
        val closed = (1..6).map { samplePlace("c$it", open = false) }
        val fetch = session.next(filters, CANBERRA) as EngineStep.Fetch
        val step = session.commit(SearchPage(open + closed, null), fetch)
        assertTrue(step is EngineStep.Fetch)
        assertTrue((step as EngineStep.Fetch).radiusKm > filters.radiusKm)
    }

    @Test
    fun radiusLadderStepsUpAndStopsAtTheCap() {
        assertEquals(1.0, RadiusPolicy.widen(0.5)!!, 0.001)
        assertEquals(5.0, RadiusPolicy.widen(3.0)!!, 0.001)
        assertEquals(25.0, RadiusPolicy.widen(20.0)!!, 0.001)
        assertEquals(null, RadiusPolicy.widen(25.0))
    }
}
