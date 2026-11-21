package au.com.uulabs.uufeed

import au.com.uulabs.uufeed.data.PlacesException
import au.com.uulabs.uufeed.data.PlacesRepository
import au.com.uulabs.uufeed.data.dto.PlaceDto
import au.com.uulabs.uufeed.data.legacy.LegacyPlacesSource
import au.com.uulabs.uufeed.domain.Budget
import au.com.uulabs.uufeed.domain.GeoPoint
import au.com.uulabs.uufeed.domain.SearchFilters
import au.com.uulabs.uufeed.domain.SuggestionSession
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The owner's Nearby Search response from Monash (legacy key, 3 km, open now).
 * Whole-number ratings such as `"rating": 4` must survive decoding, and the
 * session must still be able to pick five places.
 */
class LegacyFixturePipelineTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val origin = GeoPoint(-35.4130, 149.0900)
    private val filters = SearchFilters(
        radiusKm = 3.0,
        minRating = null,
        budget = Budget.ANY,
        keyword = "",
    )

    @Test
    fun ownerNearbyFixtureYieldsFivePicks() = runBlocking {
        val nearby = fixture("legacy_nearby_sample.json")
        val server = MockWebServer()
        val logged = mutableListOf<String>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse().setBody(nearby)
        }
        server.start()
        try {
            val source = LegacyPlacesSource(
                http = OkHttpClient(),
                apiKey = { "secret-key" },
                json = json,
                beforePageToken = {},
                debugLog = { logged += it },
                baseUrl = server.url("/"),
            )
            val page = source.search(monashQuery())
            assertTrue("mapped ${page.places.size} places from the fixture", page.places.size >= 15)
            assertTrue(page.places.any { it.name == "Subway" })
            assertTrue(page.places.all { it.latitude in -35.45..-35.35 })

            val outcome = PlacesRepository(
                source,
                SuggestionSession(random = Random(1)),
            ).load(filters, origin)
            assertEquals(5, outcome.places.size)
            assertTrue(outcome.places.all { it.name.isNotBlank() && it.id.isNotBlank() })
            assertEquals(5, outcome.places.map { it.id }.toSet().size)

            val request = server.takeRequest()
            val path = request.path.orEmpty()
            assertTrue(path.contains("keyword=restaurant%7Ccafe%7Cmeal_takeaway") || path.contains("keyword=restaurant|cafe|meal_takeaway"))
            assertTrue(path.contains("opennow=true"))
            assertTrue(path.contains("radius=3000"))
            assertTrue(logged.any { it.contains("nearbysearch") && !it.contains("secret-key") && !it.contains("key=") })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun ownerDetailsFixtureMapsFiveNewestReviews() = runBlocking {
        val details = fixture("legacy_details_sample.json")
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(details))
        server.start()
        try {
            val source = LegacyPlacesSource(
                http = OkHttpClient(),
                apiKey = { "secret-key" },
                json = json,
                beforePageToken = {},
                baseUrl = server.url("/"),
            )
            val place = source.details("ChIJA0t3x061F2sRveyFAvAkN1o")
            assertEquals("Subway", place.name)
            assertEquals(5, place.reviews.size)
            assertEquals("Faizan Ahmad آفیشل", place.reviews.first().authorName)
            assertEquals(1.0, place.reviews.first().rating!!, 0.01)
            val times = place.reviews.map { it.publishedAt!! }
            assertEquals(times.sortedDescending(), times)
            val path = server.takeRequest().path.orEmpty()
            assertTrue(path.contains("reviews_sort=newest"))
            assertTrue(path.contains("key=secret-key"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun nonOkStatusIsTheErrorNotAnEmptyPage() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """{"status":"REQUEST_DENIED","error_message":"The provided API key is invalid."}""",
            ),
        )
        server.start()
        try {
            val source = LegacyPlacesSource(
                http = OkHttpClient(),
                apiKey = { "k" },
                json = json,
                beforePageToken = {},
                baseUrl = server.url("/"),
            )
            val error = runCatching { source.search(monashQuery()) }.exceptionOrNull()
            assertTrue(error is PlacesException)
            val message = error!!.message.orEmpty()
            assertTrue(message.contains("REQUEST_DENIED"))
            assertTrue(message.contains("The provided API key is invalid."))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun newApiPlaceAcceptsWholeNumberRating() {
        val place = json.decodeFromString(
            PlaceDto.serializer(),
            """{"id":"abc","displayName":{"text":"Cafe"},"location":{"latitude":-35,"longitude":149},"rating":4,"userRatingCount":12}""",
        )
        assertEquals(4.0, place.rating!!, 0.01)
        val location = place.location!!
        assertEquals(-35.0, location.latitude, 0.01)
        assertEquals(149.0, location.longitude, 0.01)
    }

    private fun monashQuery() = au.com.uulabs.uufeed.domain.PlacesQuery(
        textQuery = "restaurants cafes",
        origin = origin,
        radiusMeters = 3000.0,
        openNow = true,
        minRating = null,
        priceLevels = null,
        pageToken = null,
        rankPreference = "RELEVANCE",
    )

    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResourceAsStream(name)!!.bufferedReader().readText()
}
