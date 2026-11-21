package com.lunchy.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
data class TextSearchRequest(
    val textQuery: String,
    val pageSize: Int = 20,
    val pageToken: String? = null,
    val openNow: Boolean = true,
    val minRating: Double? = null,
    val priceLevels: List<String>? = null,
    val locationBias: LocationBiasDto,
    val rankPreference: String,
    val languageCode: String = "en",
    val regionCode: String = "AU",
)

@Serializable
data class LocationBiasDto(
    val circle: CircleDto,
)

@Serializable
data class CircleDto(
    val center: LatLngDto,
    val radius: Double,
)

@Serializable
data class LatLngDto(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class TextSearchResponse(
    val places: List<PlaceDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class PlaceDto(
    val id: String? = null,
    val name: String? = null,
    val displayName: LocalizedTextDto? = null,
    val formattedAddress: String? = null,
    val location: LatLngDto? = null,
    val rating: Double? = null,
    val userRatingCount: Int? = null,
    val priceLevel: String? = null,
    val priceRange: PriceRangeDto? = null,
    val photos: List<PhotoDto>? = null,
    val currentOpeningHours: OpeningHoursDto? = null,
    val regularOpeningHours: OpeningHoursDto? = null,
    val nationalPhoneNumber: String? = null,
    val internationalPhoneNumber: String? = null,
    val websiteUri: String? = null,
    val googleMapsUri: String? = null,
    val primaryType: String? = null,
    val primaryTypeDisplayName: LocalizedTextDto? = null,
    val businessStatus: String? = null,
    val editorialSummary: LocalizedTextDto? = null,
    val reviews: List<ReviewDto>? = null,
)

@Serializable
data class LocalizedTextDto(
    val text: String? = null,
    val languageCode: String? = null,
)

@Serializable
data class PhotoDto(
    val name: String? = null,
    val authorAttributions: List<AuthorDto>? = null,
)

@Serializable
data class AuthorDto(
    val displayName: String? = null,
    val uri: String? = null,
    val photoUri: String? = null,
)

@Serializable
data class OpeningHoursDto(
    val openNow: Boolean? = null,
    val weekdayDescriptions: List<String>? = null,
)

@Serializable
data class PriceRangeDto(
    val startPrice: MoneyDto? = null,
    val endPrice: MoneyDto? = null,
)

@Serializable
data class MoneyDto(
    val currencyCode: String? = null,
    val units: JsonElement? = null,
    val nanos: Int? = null,
)

@Serializable
data class ReviewDto(
    val relativePublishTimeDescription: String? = null,
    val rating: Double? = null,
    val text: LocalizedTextDto? = null,
    val originalText: LocalizedTextDto? = null,
    val authorAttribution: AuthorDto? = null,
    val publishTime: String? = null,
    val googleMapsUri: String? = null,
)

@Serializable
data class GoogleErrorEnvelope(
    val error: GoogleErrorBody? = null,
)

@Serializable
data class GoogleErrorBody(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null,
)

fun JsonElement?.asLongOrNull(): Long? = when (this) {
    is JsonPrimitive -> if (isString) content.toLongOrNull() else longOrNull
    else -> null
}
