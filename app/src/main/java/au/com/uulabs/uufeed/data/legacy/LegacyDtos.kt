package au.com.uulabs.uufeed.data.legacy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LegacyEnvelope(
    val status: String? = null,
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("next_page_token") val nextPageToken: String? = null,
    val results: List<LegacyPlace> = emptyList(),
    val result: LegacyPlace? = null,
)

@Serializable
data class LegacyPlace(
    @SerialName("place_id") val placeId: String? = null,
    val name: String? = null,
    val rating: Double? = null,
    @SerialName("user_ratings_total") val userRatingsTotal: Int? = null,
    @SerialName("price_level") val priceLevel: Int? = null,
    val vicinity: String? = null,
    @SerialName("formatted_address") val formattedAddress: String? = null,
    @SerialName("formatted_phone_number") val formattedPhoneNumber: String? = null,
    @SerialName("international_phone_number") val internationalPhoneNumber: String? = null,
    val website: String? = null,
    val url: String? = null,
    val geometry: LegacyGeometry? = null,
    @SerialName("opening_hours") val openingHours: LegacyOpeningHours? = null,
    val photos: List<LegacyPhoto>? = null,
    val reviews: List<LegacyReview>? = null,
    val types: List<String>? = null,
    @SerialName("business_status") val businessStatus: String? = null,
    @SerialName("permanently_closed") val permanentlyClosed: Boolean? = null,
)

@Serializable
data class LegacyGeometry(
    val location: LegacyLatLng? = null,
)

@Serializable
data class LegacyLatLng(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
)

@Serializable
data class LegacyOpeningHours(
    @SerialName("open_now") val openNow: Boolean? = null,
    @SerialName("weekday_text") val weekdayText: List<String>? = null,
)

@Serializable
data class LegacyPhoto(
    @SerialName("photo_reference") val photoReference: String? = null,
    @SerialName("html_attributions") val htmlAttributions: List<String>? = null,
)

@Serializable
data class LegacyReview(
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    val rating: Double? = null,
    @SerialName("relative_time_description") val relativeTimeDescription: String? = null,
    val text: String? = null,
    val time: Long? = null,
    @SerialName("author_url") val authorUrl: String? = null,
)
