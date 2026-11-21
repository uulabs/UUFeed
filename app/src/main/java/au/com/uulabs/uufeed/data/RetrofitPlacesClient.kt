package au.com.uulabs.uufeed.data

import au.com.uulabs.uufeed.data.dto.CircleDto
import au.com.uulabs.uufeed.data.dto.GoogleErrorEnvelope
import au.com.uulabs.uufeed.data.dto.LatLngDto
import au.com.uulabs.uufeed.data.dto.LocationBiasDto
import au.com.uulabs.uufeed.data.dto.PlaceMapper
import au.com.uulabs.uufeed.data.dto.TextSearchRequest
import au.com.uulabs.uufeed.domain.PlaceDetails
import au.com.uulabs.uufeed.domain.PlacesQuery
import au.com.uulabs.uufeed.domain.SearchPage
import kotlinx.serialization.json.Json
import retrofit2.Response

class RetrofitPlacesClient(
    private val api: PlacesApi,
    private val apiKey: () -> String,
    private val json: Json,
) : PlacesClient {
    override suspend fun search(query: PlacesQuery): SearchPage {
        if (apiKey().isBlank()) throw MissingApiKeyException()
        val response = api.searchText(PlacesApi.SEARCH_MASK, query.toRequest())
        val body = response.bodyOrThrow(json)
        return SearchPage(
            places = body.places.mapNotNull(PlaceMapper::toCandidate),
            nextPageToken = body.nextPageToken,
        )
    }

    override suspend fun details(placeId: String): PlaceDetails {
        if (apiKey().isBlank()) throw MissingApiKeyException()
        val response = api.placeDetails(placeId, PlacesApi.DETAILS_MASK)
        val body = response.bodyOrThrow(json)
        return PlaceMapper.toDetails(body)
            ?: throw PlacesException("Google returned a place without a name or location.")
    }
}

private fun PlacesQuery.toRequest(): TextSearchRequest = TextSearchRequest(
    textQuery = textQuery,
    pageSize = 20,
    pageToken = pageToken,
    openNow = openNow,
    minRating = minRating,
    priceLevels = priceLevels,
    locationBias = LocationBiasDto(
        circle = CircleDto(
            center = LatLngDto(origin.latitude, origin.longitude),
            radius = radiusMeters,
        ),
    ),
    rankPreference = rankPreference,
    languageCode = languageCode,
    regionCode = regionCode,
)

private fun <T> Response<T>.bodyOrThrow(json: Json): T {
    if (isSuccessful) {
        return body() ?: throw PlacesException("Empty response from Places.", code())
    }
    val raw = errorBody()?.string().orEmpty()
    val parsed = runCatching { json.decodeFromString(GoogleErrorEnvelope.serializer(), raw) }.getOrNull()
    val message = parsed?.error?.message ?: "Places request failed (${code()})."
    val hint = when (code()) {
        400 -> " Check that Places API (New) is enabled for this key."
        403 -> " Check the key, billing, and that Places API (New) is enabled. " +
            "If the key is restricted to Android apps, the package is au.com.uulabs.uufeed and the SHA-1 " +
            "must match this build (./gradlew signingReport)."
        429 -> " The quota for this key is used up."
        else -> ""
    }
    throw PlacesException(message + hint, code())
}
