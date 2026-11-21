package com.uulabs.uufeed

import com.uulabs.uufeed.data.PlacesClient
import com.uulabs.uufeed.data.PlacesRepository
import com.uulabs.uufeed.domain.Budget
import com.uulabs.uufeed.domain.PlaceDetails
import com.uulabs.uufeed.domain.PlacesQuery
import com.uulabs.uufeed.domain.SearchFilters
import com.uulabs.uufeed.domain.SearchNotices
import com.uulabs.uufeed.domain.SearchPage
import com.uulabs.uufeed.domain.SuggestionSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlacesRepositoryTest {
    private val filters = SearchFilters(radiusKm = 3.0, minRating = null, budget = Budget.ANY, keyword = "ramen")

    @Test
    fun secondSearchUsesThePoolAndDoesNotRepeatIds() = runBlocking {
        val places = (1..10).map { samplePlace("p$it", kmNorth = 0.4) }
        val client = FakePlacesClient { SearchPage(places, null) }
        val repository = PlacesRepository(client, SuggestionSession(random = Random(2)))
        val first = repository.load(filters, CANBERRA)
        val second = repository.load(filters, CANBERRA)
        assertEquals(1, client.queries.size)
        assertEquals(5, first.places.size)
        assertEquals(5, second.places.size)
        assertTrue(first.places.map { it.id }.intersect(second.places.map { it.id }.toSet()).isEmpty())
        assertEquals("ramen", client.queries.single().textQuery)
        assertEquals(true, client.queries.single().openNow)
        assertEquals("RELEVANCE", client.queries.single().rankPreference)
    }

    @Test
    fun widensAndSaysSoWhenTheFirstPageIsOutsideTheRadius() = runBlocking {
        val far = samplePlace("far", kmNorth = 8.0)
        val near = (1..5).map { samplePlace("n$it", kmNorth = 0.4) }
        var calls = 0
        val client = FakePlacesClient {
            calls += 1
            if (calls == 1) SearchPage(listOf(far), null) else SearchPage(near, null)
        }
        val repository = PlacesRepository(client, SuggestionSession(random = Random(0)))
        val outcome = repository.load(filters, CANBERRA)
        assertEquals(5, outcome.places.size)
        assertTrue(outcome.effectiveRadiusKm > filters.radiusKm)
        assertEquals("DISTANCE", client.queries.last().rankPreference)
        assertTrue(SearchNotices.message(outcome)!!.contains("within", ignoreCase = true))
    }

    @Test
    fun followsTheNextPageTokenBeforeWidening() = runBlocking {
        val first = (1..2).map { samplePlace("a$it", kmNorth = 0.2) }
        val second = (1..6).map { samplePlace("b$it", kmNorth = 0.3) }
        val client = FakePlacesClient { query ->
            if (query.pageToken == null) SearchPage(first, "next") else SearchPage(second, null)
        }
        val repository = PlacesRepository(client, SuggestionSession(random = Random(1)))
        val outcome = repository.load(filters, CANBERRA)
        assertEquals(5, outcome.places.size)
        assertEquals(2, client.queries.size)
        assertEquals("next", client.queries[1].pageToken)
        assertEquals(filters.radiusKm * 1000.0, client.queries[0].radiusMeters, 0.1)
    }
}

private class FakePlacesClient(
    private val handler: (PlacesQuery) -> SearchPage,
) : PlacesClient {
    val queries = mutableListOf<PlacesQuery>()

    override suspend fun search(query: PlacesQuery): SearchPage {
        queries += query
        return handler(query)
    }

    override suspend fun details(placeId: String): PlaceDetails = error("unused")
}
