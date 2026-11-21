package au.com.uulabs.uufeed

import au.com.uulabs.uufeed.data.dto.PlaceDto
import au.com.uulabs.uufeed.data.dto.PlaceMapper
import au.com.uulabs.uufeed.data.dto.TextSearchRequest
import au.com.uulabs.uufeed.data.dto.LatLngDto
import au.com.uulabs.uufeed.data.dto.LocationBiasDto
import au.com.uulabs.uufeed.data.dto.CircleDto
import au.com.uulabs.uufeed.domain.PriceLevel
import au.com.uulabs.uufeed.domain.formatDistanceKm
import au.com.uulabs.uufeed.domain.formatKm
import au.com.uulabs.uufeed.domain.newestReviews
import au.com.uulabs.uufeed.domain.Review
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PlaceMapperTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

    @Test
    fun mapsPriceHoursPhotosAndSortsReviewsNewestFirst() {
        val dto = json.decodeFromString(PlaceDto.serializer(), SAMPLE)
        val details = PlaceMapper.toDetails(dto)!!
        assertEquals("Two Before Ten", details.name)
        assertEquals(PriceLevel.MODERATE, details.priceLevel)
        assertEquals("$$ · A$20–A$40", details.priceLabel)
        assertEquals(false, details.openNow)
        assertEquals("Cafe", details.typeLabel)
        assertEquals(1, details.photos.size)
        assertEquals("Photo: Alex", details.photos.first().attribution)
        assertEquals(listOf("Sam", "Alex"), details.reviews.map { it.authorName })
        assertTrue(details.reviews.first().publishedAt!! > details.reviews.last().publishedAt!!)
        assertEquals("Great flat white.", details.reviews.first().text)
    }

    @Test
    fun permanentlyClosedPlacesAreTreatedAsClosed() {
        val dto = json.decodeFromString(
            PlaceDto.serializer(),
            """
            {"id":"x","displayName":{"text":"Gone"},"location":{"latitude":1,"longitude":2},"businessStatus":"CLOSED_PERMANENTLY","currentOpeningHours":{"openNow":true}}
            """.trimIndent(),
        )
        assertEquals(false, PlaceMapper.toCandidate(dto)!!.openNow)
    }

    @Test
    fun requestOmitsUnsetRatingAndPrice() {
        val request = TextSearchRequest(
            textQuery = "restaurants cafes",
            openNow = true,
            minRating = null,
            priceLevels = null,
            locationBias = LocationBiasDto(CircleDto(LatLngDto(-35.28, 149.13), 3000.0)),
            rankPreference = "RELEVANCE",
        )
        val text = json.encodeToString(TextSearchRequest.serializer(), request)
        assertFalse(text.contains("minRating"))
        assertFalse(text.contains("priceLevels"))
        assertFalse(text.contains("pageToken"))
        assertTrue(text.contains("\"openNow\":true"))
        assertTrue(text.contains("\"regionCode\":\"AU\""))
    }

    @Test
    fun reviewsWithoutTimestampsSinkButStayInOrder() {
        val older = review("older", "2020-01-01T00:00:00Z")
        val newer = review("newer", "2026-05-01T00:00:00Z")
        val undatedA = review("a", null)
        val undatedB = review("b", null)
        val sorted = newestReviews(listOf(undatedA, older, undatedB, newer))
        assertEquals(listOf("newer", "older", "a", "b"), sorted.map { it.authorName })
    }

    @Test
    fun distanceAndRadiusUseMetricUnits() {
        assertEquals("0 m", formatDistanceKm(0.0))
        assertEquals("850 m", formatDistanceKm(0.85))
        assertEquals("1.2 km", formatDistanceKm(1.16))
        assertEquals("3 km", formatKm(3.0))
        assertEquals("0.5 km", formatKm(0.5))
        assertNull(PlaceMapper.toCandidate(PlaceDto(id = "x", displayName = au.com.uulabs.uufeed.data.dto.LocalizedTextDto("Nameless"))))
    }

    private fun review(name: String, published: String?) = Review(
        authorName = name,
        authorPhotoUrl = null,
        rating = 5.0,
        relativeTime = null,
        text = name,
        publishedAt = published?.let(Instant::parse),
        mapsUri = null,
    )

    private companion object {
        val SAMPLE = """
            {
              "id": "abc",
              "displayName": {"text": "Two Before Ten", "languageCode": "en"},
              "formattedAddress": "1 London Circuit, Canberra",
              "location": {"latitude": -35.28, "longitude": 149.13},
              "rating": 4.4,
              "userRatingCount": 320,
              "priceLevel": "PRICE_LEVEL_MODERATE",
              "priceRange": {
                "startPrice": {"currencyCode": "AUD", "units": "20"},
                "endPrice": {"currencyCode": "AUD", "units": "40", "nanos": 0}
              },
              "photos": [
                {"name": "places/abc/photos/pic", "authorAttributions": [{"displayName": "Alex"}]}
              ],
              "currentOpeningHours": {"openNow": false},
              "primaryType": "cafe",
              "primaryTypeDisplayName": {"text": "Cafe"},
              "businessStatus": "OPERATIONAL",
              "reviews": [
                {
                  "rating": 4,
                  "text": {"text": "Quiet on a Tuesday."},
                  "authorAttribution": {"displayName": "Alex", "photoUri": "https://example.com/a.jpg"},
                  "publishTime": "2024-01-02T00:00:00Z",
                  "relativePublishTimeDescription": "2 years ago"
                },
                {
                  "rating": 5,
                  "text": {"text": "Great flat white."},
                  "authorAttribution": {"displayName": "Sam", "photoUri": "https://example.com/s.jpg"},
                  "publishTime": "2026-10-01T12:00:00Z",
                  "relativePublishTimeDescription": "a month ago"
                }
              ],
              "extraField": "ignored"
            }
        """.trimIndent()
    }
}
