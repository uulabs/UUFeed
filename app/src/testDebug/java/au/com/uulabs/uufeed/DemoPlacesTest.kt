package au.com.uulabs.uufeed

import au.com.uulabs.uufeed.data.PlacesRepository
import au.com.uulabs.uufeed.demo.DemoMode
import au.com.uulabs.uufeed.demo.DemoPlacesClient
import au.com.uulabs.uufeed.domain.Budget
import au.com.uulabs.uufeed.domain.Geo
import au.com.uulabs.uufeed.domain.GeoPoint
import au.com.uulabs.uufeed.domain.PlacesQuery
import au.com.uulabs.uufeed.domain.SearchFilters
import au.com.uulabs.uufeed.domain.SuggestionSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoPlacesTest {
    @Test
    fun openPlacesNearCivicCoverTwoBatches() = runBlocking {
        val client = DemoPlacesClient()
        val page = client.search(
            PlacesQuery(
                textQuery = "restaurants cafes",
                origin = DemoMode.origin,
                radiusMeters = 3000.0,
                openNow = true,
                minRating = null,
                priceLevels = null,
                pageToken = null,
                rankPreference = "RELEVANCE",
            ),
        )
        assertTrue(page.places.none { it.name == "Old Civic Canteen" })
        assertTrue(page.places.size >= 10)
        assertTrue(page.places.all { Geo.distanceKm(DemoMode.origin, GeoPoint(it.latitude, it.longitude)) <= 3.0 })

        val repository = PlacesRepository(client, SuggestionSession())
        val filters = SearchFilters(3.0, null, Budget.ANY, "")
        val first = repository.load(filters, DemoMode.origin)
        val second = repository.load(filters, DemoMode.origin)
        assertEquals(5, first.places.size)
        assertEquals(5, second.places.size)
        assertTrue(first.places.map { it.id }.intersect(second.places.map { it.id }.toSet()).isEmpty())
        assertTrue(first.places.all { it.photos.isNotEmpty() })
    }

    @Test
    fun ramenQueryKeepsTheRamenShop() = runBlocking {
        val page = DemoPlacesClient().search(
            PlacesQuery(
                textQuery = "ramen",
                origin = DemoMode.origin,
                radiusMeters = 3000.0,
                openNow = true,
                minRating = null,
                priceLevels = null,
                pageToken = null,
                rankPreference = "RELEVANCE",
            ),
        )
        assertEquals(listOf("London Circuit Ramen"), page.places.map { it.name })
    }

    @Test
    fun detailsSortReviewsNewestFirst() = runBlocking {
        val details = DemoPlacesClient().details("demo-lonsdale-loaf")
        val times = details.reviews.map { it.publishedAt!! }
        assertEquals(times.sortedDescending(), times)
        assertEquals(5, details.reviews.size)
        assertTrue(details.reviews.all { it.authorPhotoUrl?.startsWith("android.resource://") == true })
    }
}
