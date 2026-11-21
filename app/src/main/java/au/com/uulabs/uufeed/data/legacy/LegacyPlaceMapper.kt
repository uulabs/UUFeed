package au.com.uulabs.uufeed.data.legacy

import au.com.uulabs.uufeed.domain.PlaceCandidate
import au.com.uulabs.uufeed.domain.PlaceDetails
import au.com.uulabs.uufeed.domain.PlacePhoto
import au.com.uulabs.uufeed.domain.PriceLevel
import au.com.uulabs.uufeed.domain.Review
import au.com.uulabs.uufeed.domain.formatPrice
import au.com.uulabs.uufeed.domain.newestReviews
import java.time.Instant

object LegacyPlaceMapper {
    fun toCandidate(place: LegacyPlace): PlaceCandidate? {
        val id = place.placeId?.takeIf { it.isNotBlank() } ?: return null
        val location = place.geometry?.location ?: return null
        val name = place.name?.takeIf { it.isNotBlank() } ?: return null
        val level = priceLevel(place.priceLevel)
        return PlaceCandidate(
            id = id,
            name = name,
            rating = place.rating,
            reviewCount = place.userRatingsTotal ?: 0,
            priceLevel = level,
            priceLabel = formatPrice(level, null),
            latitude = location.lat,
            longitude = location.lng,
            openNow = openNow(place),
            primaryType = primaryType(place.types),
            typeLabel = typeLabel(place.types),
            address = place.formattedAddress?.takeIf { it.isNotBlank() } ?: place.vicinity,
            photos = photos(place),
            businessStatus = place.businessStatus,
        )
    }

    fun toDetails(place: LegacyPlace, fallbackId: String = ""): PlaceDetails {
        val candidate = toCandidate(place)
        val level = priceLevel(place.priceLevel)
        return PlaceDetails(
            id = candidate?.id ?: place.placeId?.takeIf { it.isNotBlank() } ?: fallbackId,
            name = candidate?.name ?: place.name?.takeIf { it.isNotBlank() }.orEmpty(),
            address = candidate?.address
                ?: place.formattedAddress?.takeIf { it.isNotBlank() }
                ?: place.vicinity,
            phone = place.formattedPhoneNumber ?: place.internationalPhoneNumber,
            internationalPhone = place.internationalPhoneNumber ?: place.formattedPhoneNumber,
            website = place.website,
            mapsUri = place.url,
            rating = candidate?.rating ?: place.rating,
            reviewCount = candidate?.reviewCount ?: place.userRatingsTotal ?: 0,
            priceLabel = candidate?.priceLabel ?: formatPrice(level, null),
            priceLevel = candidate?.priceLevel ?: level,
            openNow = candidate?.openNow ?: openNow(place),
            weekdayHours = place.openingHours?.weekdayText.orEmpty(),
            photos = candidate?.photos ?: photos(place),
            reviews = newestReviews(reviews(place)),
            editorial = null,
            typeLabel = candidate?.typeLabel ?: typeLabel(place.types),
            latitude = candidate?.latitude ?: place.geometry?.location?.lat ?: 0.0,
            longitude = candidate?.longitude ?: place.geometry?.location?.lng ?: 0.0,
        )
    }

    private fun priceLevel(level: Int?): PriceLevel? = when (level) {
        0 -> PriceLevel.FREE
        1 -> PriceLevel.INEXPENSIVE
        2 -> PriceLevel.MODERATE
        3 -> PriceLevel.EXPENSIVE
        4 -> PriceLevel.VERY_EXPENSIVE
        else -> null
    }

    private fun openNow(place: LegacyPlace): Boolean? {
        if (place.permanentlyClosed == true) return false
        if (place.businessStatus == "CLOSED_PERMANENTLY" || place.businessStatus == "CLOSED_TEMPORARILY") {
            return false
        }
        return place.openingHours?.openNow
    }

    private fun primaryType(types: List<String>?): String? =
        types.orEmpty().firstOrNull { it !in GENERIC_TYPES }

    private fun typeLabel(types: List<String>?): String? =
        primaryType(types)
            ?.replace('_', ' ')
            ?.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }

    private fun photos(place: LegacyPlace): List<PlacePhoto> =
        place.photos.orEmpty().mapNotNull { photo ->
            val reference = photo.photoReference?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val author = photo.htmlAttributions.orEmpty()
                .firstNotNullOfOrNull { stripHtml(it).takeIf(String::isNotBlank) }
            PlacePhoto(
                name = "legacy:$reference",
                attribution = author?.let { "Photo: $it" },
            )
        }

    private fun reviews(place: LegacyPlace): List<Review> =
        place.reviews.orEmpty().map { review ->
            Review(
                authorName = review.authorName?.takeIf { it.isNotBlank() } ?: "Google user",
                authorPhotoUrl = review.profilePhotoUrl,
                rating = review.rating,
                relativeTime = review.relativeTimeDescription,
                text = review.text.orEmpty(),
                publishedAt = review.time?.let { Instant.ofEpochSecond(it) },
                mapsUri = review.authorUrl,
            )
        }

    private fun stripHtml(raw: String): String = raw.replace(TAG, "").trim()

    private val TAG = Regex("<[^>]*>")
    private val GENERIC_TYPES = setOf("point_of_interest", "establishment", "food")
}
