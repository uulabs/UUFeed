package com.lunchy.data

import com.lunchy.domain.PlaceDetails
import com.lunchy.domain.PlacesQuery
import com.lunchy.domain.SearchPage

interface PlacesClient {
    suspend fun search(query: PlacesQuery): SearchPage

    suspend fun details(placeId: String): PlaceDetails
}

open class PlacesException(
    message: String,
    val code: Int? = null,
) : Exception(message)

class MissingApiKeyException : PlacesException(
    "Lunchy needs a Google Places API key. Add MAPS_API_KEY to local.properties, then rebuild and reinstall the app.",
)
