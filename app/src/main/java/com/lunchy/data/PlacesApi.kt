package com.lunchy.data

import com.lunchy.data.dto.PlaceDto
import com.lunchy.data.dto.TextSearchRequest
import com.lunchy.data.dto.TextSearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PlacesApi {
    @POST("v1/places:searchText")
    suspend fun searchText(
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body body: TextSearchRequest,
    ): Response<TextSearchResponse>

    @GET("v1/places/{placeId}")
    suspend fun placeDetails(
        @Path("placeId") placeId: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Query("languageCode") languageCode: String = "en",
        @Query("regionCode") regionCode: String = "AU",
    ): Response<PlaceDto>

    companion object {
        val SEARCH_MASK = listOf(
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.rating",
            "places.userRatingCount",
            "places.priceLevel",
            "places.priceRange",
            "places.photos",
            "places.currentOpeningHours.openNow",
            "places.primaryType",
            "places.primaryTypeDisplayName",
            "places.businessStatus",
            "nextPageToken",
        ).joinToString(",")

        val DETAILS_MASK = listOf(
            "id",
            "displayName",
            "formattedAddress",
            "location",
            "rating",
            "userRatingCount",
            "priceLevel",
            "priceRange",
            "photos",
            "currentOpeningHours",
            "regularOpeningHours",
            "nationalPhoneNumber",
            "internationalPhoneNumber",
            "websiteUri",
            "googleMapsUri",
            "primaryType",
            "primaryTypeDisplayName",
            "editorialSummary",
            "reviews",
            "businessStatus",
        ).joinToString(",")
    }
}
