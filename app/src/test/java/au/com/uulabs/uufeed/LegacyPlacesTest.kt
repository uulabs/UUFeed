package au.com.uulabs.uufeed

import au.com.uulabs.uufeed.data.KeyTestResult
import au.com.uulabs.uufeed.data.PlacesBackend
import au.com.uulabs.uufeed.data.PlacesKeyTester
import au.com.uulabs.uufeed.data.legacy.LegacyEnvelope
import au.com.uulabs.uufeed.data.legacy.LegacyPlaceMapper
import au.com.uulabs.uufeed.data.legacy.LegacyPlacesSource
import au.com.uulabs.uufeed.domain.Budget
import au.com.uulabs.uufeed.domain.GeoPoint
import au.com.uulabs.uufeed.domain.PlacesQuery
import au.com.uulabs.uufeed.domain.PriceLevel
import au.com.uulabs.uufeed.domain.apiPriceLevels
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyPlacesTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun nearbyJsonMapsPlacePricePhotoAndMarksClosed() {
        val envelope = json.decodeFromString(LegacyEnvelope.serializer(), NEARBY)
        val places = envelope.results.mapNotNull(LegacyPlaceMapper::toCandidate)
        assertEquals(false, places.first { it.id == "closed" }.openNow)
        val cafe = places.first { it.id == "cafe-1" }
        assertEquals("cafe-1", cafe.id)
        assertEquals("Two Before Ten", cafe.name)
        assertEquals(4.4, cafe.rating!!, 0.01)
        assertEquals(86, cafe.reviewCount)
        assertEquals(PriceLevel.MODERATE, cafe.priceLevel)
        assertEquals("$$", cafe.priceLabel)
        assertEquals(true, cafe.openNow)
        assertEquals(-35.281, cafe.latitude, 0.0001)
        assertEquals(149.131, cafe.longitude, 0.0001)
        assertEquals("12 London Cct", cafe.address)
        assertEquals("Cafe", cafe.typeLabel)
        assertEquals("legacy:photo-ref", cafe.photos.single().name)
        assertEquals("Photo: Alex", cafe.photos.single().attribution)
        assertTrue(cafe.photos.single().mediaUrl(400).startsWith("https://maps.googleapis.com/maps/api/place/photo?"))
        assertTrue(cafe.photos.single().mediaUrl(400).contains("photo_reference=photo-ref"))
        assertFalse(cafe.photos.single().mediaUrl(400).contains("key="))
        assertEquals("next-page", envelope.nextPageToken)
    }

    @Test
    fun detailsJsonKeepsNewestReviewOrderAndContactFields() {
        val envelope = json.decodeFromString(LegacyEnvelope.serializer(), DETAILS)
        val details = LegacyPlaceMapper.toDetails(envelope.result!!)
        assertEquals("Two Before Ten", details.name)
        assertEquals("02 6162 0000", details.phone)
        assertEquals("+61 2 6162 0000", details.internationalPhone)
        assertEquals("https://twobeforeten.com.au", details.website)
        assertEquals("https://maps.google.com/?cid=1", details.mapsUri)
        assertEquals(listOf("Monday: 7:00 AM – 3:00 PM"), details.weekdayHours)
        assertEquals(listOf("Sam", "Alex"), details.reviews.map { it.authorName })
        assertTrue(details.reviews.first().publishedAt!! > details.reviews.last().publishedAt!!)
        assertEquals("A flat white.", details.reviews.first().text)
    }

    @Test
    fun nearbyUrlUsesKeywordPriceAndOpenNow() {
        val url = LegacyPlacesSource.nearbyUrl(
            LegacyPlacesSource.DEFAULT_BASE,
            PlacesQuery(
                textQuery = "ramen",
                origin = GeoPoint(-35.28, 149.13),
                radiusMeters = 1500.0,
                openNow = true,
                minRating = 4.0,
                priceLevels = Budget.MODERATE.apiPriceLevels(),
                pageToken = null,
                rankPreference = "RELEVANCE",
            ),
            "test-key",
        )
        val text = url.toString()
        assertTrue(text.contains("opennow=true"))
        assertTrue(text.contains("keyword=ramen"))
        assertTrue(text.contains("minprice=0"))
        assertTrue(text.contains("maxprice=2"))
        assertTrue(text.contains("radius=1500"))
        assertTrue(text.contains("location=-35.28%2C149.13") || text.contains("location=-35.28,149.13"))
        assertFalse(text.contains("type="))
        assertFalse(text.contains("minrating"))
    }

    @Test
    fun defaultSearchKeywordCoversRestaurantCafeAndTakeaway() {
        val url = LegacyPlacesSource.nearbyUrl(
            LegacyPlacesSource.DEFAULT_BASE,
            PlacesQuery(
                textQuery = "restaurants cafes",
                origin = GeoPoint(1.0, 2.0),
                radiusMeters = 800.0,
                openNow = true,
                minRating = null,
                priceLevels = null,
                pageToken = null,
                rankPreference = "RELEVANCE",
            ),
            "k",
        )
        assertTrue(url.queryParameter("keyword")!!.contains("restaurant"))
        assertTrue(url.queryParameter("keyword")!!.contains("cafe"))
        assertTrue(url.queryParameter("keyword")!!.contains("meal_takeaway"))
        assertNull(url.queryParameter("minprice"))
    }

    @Test
    fun distanceRankOmitsRadiusAndPageTokenStandsAlone() {
        val distance = LegacyPlacesSource.nearbyUrl(
            LegacyPlacesSource.DEFAULT_BASE,
            sampleQuery(rank = "DISTANCE", token = null),
            "k",
        )
        assertEquals("distance", distance.queryParameter("rankby"))
        assertNull(distance.queryParameter("radius"))
        val page = LegacyPlacesSource.nearbyUrl(
            LegacyPlacesSource.DEFAULT_BASE,
            sampleQuery(rank = "RELEVANCE", token = "tok"),
            "k",
        )
        assertEquals("tok", page.queryParameter("pagetoken"))
        assertNull(page.queryParameter("radius"))
        assertNull(page.queryParameter("keyword"))
    }

    @Test
    fun detailsUrlAsksForNewestReviews() {
        val url = LegacyPlacesSource.detailsUrl(LegacyPlacesSource.DEFAULT_BASE, "cafe-1", "k")
        assertEquals("newest", url.queryParameter("reviews_sort"))
        assertEquals("cafe-1", url.queryParameter("place_id"))
        val fields = url.queryParameter("fields").orEmpty()
        listOf(
            "name",
            "rating",
            "user_ratings_total",
            "price_level",
            "formatted_address",
            "formatted_phone_number",
            "website",
            "opening_hours",
            "photo",
            "reviews",
            "url",
            "place_id",
            "geometry/location",
        ).forEach { field -> assertTrue("$field missing from $fields", fields.contains(field)) }
    }

    @Test
    fun permissionDeniedFallsThroughToLegacy() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(403).setBody(PERMISSION_DENIED))
        server.enqueue(MockResponse().setBody("""{"status":"ZERO_RESULTS","results":[]}"""))
        server.start()
        try {
            val tester = tester(server)
            val result = tester.test("legacy-key")
            assertTrue(result is KeyTestResult.Success)
            assertEquals(PlacesBackend.LEGACY, (result as KeyTestResult.Success).backend)
            assertEquals("Using Google Places (legacy).", result.message)
            val first = server.takeRequest()
            assertEquals("/v1/places:searchText", first.path)
            assertEquals("legacy-key", first.getHeader("X-Goog-Api-Key"))
            val second = server.takeRequest()
            assertTrue(second.path!!.startsWith("/maps/api/place/nearbysearch/json"))
            assertTrue(second.path!!.contains("key=legacy-key"))
            assertTrue(second.path!!.contains("opennow=true"))
            assertEquals(2, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun newApiSuccessDoesNotCallLegacy() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"places":[{"id":"abc"}]}"""))
        server.start()
        try {
            val result = tester(server).test("new-key")
            assertEquals(PlacesBackend.NEW, (result as KeyTestResult.Success).backend)
            assertTrue(result.message.contains("Places (New)"))
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun bothApisDeniedIsOneClearFailure() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(403).setBody(PERMISSION_DENIED))
        server.enqueue(
            MockResponse().setBody(
                """{"status":"REQUEST_DENIED","error_message":"The provided API key is invalid."}""",
            ),
        )
        server.start()
        try {
            val result = tester(server).test("nope")
            assertTrue(result is KeyTestResult.Failed)
            val message = (result as KeyTestResult.Failed).message
            assertTrue(message.contains("Places API (New)"))
            assertTrue(message.contains("legacy"))
            assertTrue(message.contains("prepay"))
            assertEquals(2, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun quotaFailureDoesNotPretendTheLegacyApiIsNeeded() {
        assertFalse(
            PlacesKeyTester.newApiNeedsLegacyFallback(
                httpCode = 429,
                status = "RESOURCE_EXHAUSTED",
                message = "Quota exceeded",
            ),
        )
        assertTrue(
            PlacesKeyTester.newApiNeedsLegacyFallback(
                httpCode = 403,
                status = "PERMISSION_DENIED",
                message = "Places API (New) has not been used in project 1 before or it is disabled.",
            ),
        )
        assertFalse(
            PlacesKeyTester.newApiNeedsLegacyFallback(
                httpCode = 400,
                status = "INVALID_ARGUMENT",
                message = "API key not valid. Please pass a valid API key.",
            ),
        )
    }

    private fun tester(server: MockWebServer) = PlacesKeyTester(
        http = OkHttpClient(),
        packageName = "au.com.uulabs.uufeed",
        certSha1 = "AA",
        json = json,
        newSearchUrl = server.url("/v1/places:searchText").toString(),
        legacyNearbyUrl = server.url("/maps/api/place/nearbysearch/json").toString(),
    )

    private fun sampleQuery(rank: String, token: String?) = PlacesQuery(
        textQuery = "ramen",
        origin = GeoPoint(1.0, 2.0),
        radiusMeters = 1000.0,
        openNow = true,
        minRating = null,
        priceLevels = null,
        pageToken = token,
        rankPreference = rank,
    )
}

private const val PERMISSION_DENIED = """
{"error":{"code":403,"message":"Places API (New) has not been used in project 1 before or it is disabled.","status":"PERMISSION_DENIED"}}
"""

private const val NEARBY = """
{
  "status": "OK",
  "next_page_token": "next-page",
  "results": [
    {
      "place_id": "closed",
      "name": "Shut",
      "business_status": "CLOSED_TEMPORARILY",
      "geometry": {"location": {"lat": 0, "lng": 0}},
      "opening_hours": {"open_now": true}
    },
    {
      "place_id": "cafe-1",
      "name": "Two Before Ten",
      "rating": 4.4,
      "user_ratings_total": 86,
      "price_level": 2,
      "vicinity": "12 London Cct",
      "geometry": {"location": {"lat": -35.281, "lng": 149.131}},
      "opening_hours": {"open_now": true},
      "types": ["cafe", "food", "point_of_interest"],
      "photos": [{
        "photo_reference": "photo-ref",
        "html_attributions": ["<a href=\"https://maps.google.com\">Alex</a>"]
      }]
    }
  ]
}
"""

private const val DETAILS = """
{
  "status": "OK",
  "result": {
    "place_id": "cafe-1",
    "name": "Two Before Ten",
    "rating": 4.4,
    "user_ratings_total": 86,
    "price_level": 2,
    "formatted_address": "12 London Circuit, Canberra",
    "formatted_phone_number": "02 6162 0000",
    "international_phone_number": "+61 2 6162 0000",
    "website": "https://twobeforeten.com.au",
    "url": "https://maps.google.com/?cid=1",
    "geometry": {"location": {"lat": -35.281, "lng": 149.131}},
    "opening_hours": {
      "open_now": true,
      "weekday_text": ["Monday: 7:00 AM – 3:00 PM"]
    },
    "reviews": [
      {"author_name": "Alex", "rating": 4, "text": "Older.", "time": 1700000000, "relative_time_description": "a year ago"},
      {"author_name": "Sam", "rating": 5, "text": "A flat white.", "time": 1710000000, "relative_time_description": "a week ago", "profile_photo_url": "https://example.com/sam.jpg"}
    ]
  }
}
"""
