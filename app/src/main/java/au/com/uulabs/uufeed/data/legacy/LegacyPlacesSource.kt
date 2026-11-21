package au.com.uulabs.uufeed.data.legacy

import au.com.uulabs.uufeed.data.MissingApiKeyException
import au.com.uulabs.uufeed.data.PlacesClient
import au.com.uulabs.uufeed.data.PlacesException
import au.com.uulabs.uufeed.domain.PlaceDetails
import au.com.uulabs.uufeed.domain.PlacesQuery
import au.com.uulabs.uufeed.domain.PriceLevel
import au.com.uulabs.uufeed.domain.SearchPage
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Legacy Places API (maps.googleapis.com): Nearby Search, Place Details, and
 * Place Photo references. Nearby Search accepts one type, so a keyword covers
 * restaurant, cafe, and takeaway together. A craving replaces that keyword.
 * Details asks for reviews_sort=newest, which is the five most recent reviews.
 */
class LegacyPlacesSource(
    private val http: OkHttpClient,
    private val apiKey: () -> String,
    private val packageName: String = "",
    private val certSha1: String = "",
    private val json: Json,
    private val beforePageToken: suspend () -> Unit = { delay(PAGE_TOKEN_WAIT_MS) },
    private val baseUrl: HttpUrl = DEFAULT_BASE,
) : PlacesClient {
    override suspend fun search(query: PlacesQuery): SearchPage {
        val key = requireKey()
        if (query.pageToken != null) beforePageToken()
        val envelope = get(nearbyUrl(baseUrl, query, key))
        return SearchPage(
            places = envelope.results.mapNotNull(LegacyPlaceMapper::toCandidate),
            nextPageToken = envelope.nextPageToken?.takeIf { it.isNotBlank() },
        )
    }

    override suspend fun details(placeId: String): PlaceDetails {
        val key = requireKey()
        val envelope = get(detailsUrl(baseUrl, placeId, key))
        val place = envelope.result ?: throw PlacesException("Google returned a place without details.")
        return LegacyPlaceMapper.toDetails(place)
            ?: throw PlacesException("Google returned a place without a name or location.")
    }

    private fun requireKey(): String {
        val key = apiKey().trim()
        if (key.isBlank()) throw MissingApiKeyException()
        return key
    }

    private fun get(url: HttpUrl): LegacyEnvelope {
        val request = Request.Builder().url(url).get().apply {
            if (packageName.isNotBlank()) header("X-Android-Package", packageName)
            if (certSha1.isNotBlank()) header("X-Android-Cert", certSha1)
        }.build()
        val response = http.newCall(request).execute()
        val raw = response.use { it.body?.string().orEmpty() }
        val envelope = runCatching { json.decodeFromString(LegacyEnvelope.serializer(), raw) }.getOrNull()
        val status = envelope?.status
        if (response.isSuccessful && (status == null || status == "OK" || status == "ZERO_RESULTS")) {
            return envelope ?: LegacyEnvelope()
        }
        throw PlacesException(legacyError(status, envelope?.errorMessage, response.code), response.code)
    }

    companion object {
        const val PAGE_TOKEN_WAIT_MS = 2_000L
        val DEFAULT_BASE: HttpUrl = "https://maps.googleapis.com/".toHttpUrl()
        const val DEFAULT_KEYWORD = "restaurant cafe meal_takeaway"
        const val DETAIL_FIELDS = "name,rating,user_ratings_total,price_level,formatted_address," +
            "formatted_phone_number,international_phone_number,website,opening_hours,photos,reviews," +
            "url,geometry,types"

        fun nearbyUrl(base: HttpUrl, query: PlacesQuery, key: String): HttpUrl {
            val builder = base.newBuilder()
                .addPathSegments("maps/api/place/nearbysearch/json")
                .addQueryParameter("key", key)
            val token = query.pageToken?.takeIf { it.isNotBlank() }
            if (token != null) {
                builder.addQueryParameter("pagetoken", token)
                return builder.build()
            }
            builder.addQueryParameter(
                "location",
                "${query.origin.latitude},${query.origin.longitude}",
            )
            if (query.openNow) builder.addQueryParameter("opennow", "true")
            if (query.rankPreference == "DISTANCE") {
                builder.addQueryParameter("rankby", "distance")
            } else {
                builder.addQueryParameter(
                    "radius",
                    query.radiusMeters.toInt().coerceIn(1, 50_000).toString(),
                )
            }
            val craving = query.textQuery.trim()
            val keyword = if (craving.isEmpty() || craving == "restaurants cafes") {
                DEFAULT_KEYWORD
            } else {
                craving
            }
            builder.addQueryParameter("keyword", keyword)
            legacyPriceBounds(query.priceLevels)?.let { (min, max) ->
                builder.addQueryParameter("minprice", min.toString())
                builder.addQueryParameter("maxprice", max.toString())
            }
            return builder.build()
        }

        fun detailsUrl(base: HttpUrl, placeId: String, key: String): HttpUrl =
            base.newBuilder()
                .addPathSegments("maps/api/place/details/json")
                .addQueryParameter("place_id", placeId)
                .addQueryParameter("fields", DETAIL_FIELDS)
                .addQueryParameter("reviews_sort", "newest")
                .addQueryParameter("language", "en")
                .addQueryParameter("key", key)
                .build()

        fun legacyPriceBounds(levels: List<String>?): Pair<Int, Int>? {
            val ranks = levels.orEmpty().mapNotNull { PriceLevel.fromApi(it)?.rank }
            if (ranks.isEmpty()) return null
            return ranks.min() to ranks.max()
        }

        fun legacyError(status: String?, message: String?, httpCode: Int): String {
            val detail = message?.takeIf { it.isNotBlank() } ?: status ?: "Places request failed ($httpCode)."
            val hint = when (status) {
                "OVER_QUERY_LIMIT" -> " The quota for this key is used up."
                "REQUEST_DENIED" -> " Enable Places API (the legacy one) for this key, or turn on Places API (New)."
                "INVALID_REQUEST" -> " The legacy Places request was rejected."
                else -> ""
            }
            return detail + hint
        }
    }
}
