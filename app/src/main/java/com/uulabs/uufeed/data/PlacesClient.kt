package com.uulabs.uufeed.data

import com.uulabs.uufeed.domain.PlaceDetails
import com.uulabs.uufeed.domain.PlacesQuery
import com.uulabs.uufeed.domain.SearchPage

interface PlacesClient {
    suspend fun search(query: PlacesQuery): SearchPage

    suspend fun details(placeId: String): PlaceDetails
}

open class PlacesException(
    message: String,
    val code: Int? = null,
) : Exception(message)

class MissingApiKeyException : PlacesException(
    "UU Feed needs a Google Places API key. Open Settings and paste one, or set MAPS_API_KEY in local.properties before building.",
)
