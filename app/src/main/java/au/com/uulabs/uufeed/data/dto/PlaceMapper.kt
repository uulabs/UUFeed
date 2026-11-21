package au.com.uulabs.uufeed.data.dto

import au.com.uulabs.uufeed.domain.Money
import au.com.uulabs.uufeed.domain.PlaceCandidate
import au.com.uulabs.uufeed.domain.PlaceDetails
import au.com.uulabs.uufeed.domain.PlacePhoto
import au.com.uulabs.uufeed.domain.PriceLevel
import au.com.uulabs.uufeed.domain.PriceQuote
import au.com.uulabs.uufeed.domain.Review
import au.com.uulabs.uufeed.domain.formatPrice
import au.com.uulabs.uufeed.domain.newestReviews
import java.time.Instant

object PlaceMapper {
    fun toCandidate(dto: PlaceDto): PlaceCandidate? {
        val id = placeId(dto) ?: return null
        val location = dto.location ?: return null
        val name = dto.displayName?.text?.takeIf { it.isNotBlank() } ?: return null
        val level = PriceLevel.fromApi(dto.priceLevel)
        val quote = quote(dto.priceRange)
        return PlaceCandidate(
            id = id,
            name = name,
            rating = dto.rating,
            reviewCount = dto.userRatingCount ?: 0,
            priceLevel = level,
            priceLabel = formatPrice(level, quote),
            latitude = location.latitude,
            longitude = location.longitude,
            openNow = openNow(dto),
            primaryType = dto.primaryType,
            typeLabel = typeLabel(dto),
            address = dto.formattedAddress,
            photos = photos(dto),
            businessStatus = dto.businessStatus,
        )
    }

    fun toDetails(dto: PlaceDto): PlaceDetails? {
        val candidate = toCandidate(dto) ?: return null
        val hours = dto.regularOpeningHours?.weekdayDescriptions
            ?: dto.currentOpeningHours?.weekdayDescriptions
            ?: emptyList()
        return PlaceDetails(
            id = candidate.id,
            name = candidate.name,
            address = candidate.address,
            phone = dto.nationalPhoneNumber ?: dto.internationalPhoneNumber,
            internationalPhone = dto.internationalPhoneNumber ?: dto.nationalPhoneNumber,
            website = dto.websiteUri,
            mapsUri = dto.googleMapsUri,
            rating = candidate.rating,
            reviewCount = candidate.reviewCount,
            priceLabel = candidate.priceLabel,
            priceLevel = candidate.priceLevel,
            openNow = candidate.openNow,
            weekdayHours = hours,
            photos = candidate.photos,
            reviews = newestReviews(reviews(dto)),
            editorial = dto.editorialSummary?.text?.takeIf { it.isNotBlank() },
            typeLabel = candidate.typeLabel,
            latitude = candidate.latitude,
            longitude = candidate.longitude,
        )
    }

    private fun placeId(dto: PlaceDto): String? {
        val direct = dto.id?.takeIf { it.isNotBlank() }
        if (direct != null) return direct
        return dto.name?.substringAfterLast('/')?.takeIf { it.isNotBlank() && it != dto.name }
    }

    private fun openNow(dto: PlaceDto): Boolean? {
        val closed = dto.businessStatus == "CLOSED_PERMANENTLY" || dto.businessStatus == "CLOSED_TEMPORARILY"
        if (closed) return false
        return dto.currentOpeningHours?.openNow
    }

    private fun typeLabel(dto: PlaceDto): String? {
        val display = dto.primaryTypeDisplayName?.text?.takeIf { it.isNotBlank() }
        if (display != null) return display
        return dto.primaryType
            ?.replace('_', ' ')
            ?.replaceFirstChar { char -> char.titlecase() }
            ?.takeIf { it.isNotBlank() }
    }

    private fun photos(dto: PlaceDto): List<PlacePhoto> =
        dto.photos.orEmpty().mapNotNull { photo ->
            val name = photo.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val author = photo.authorAttributions?.firstOrNull()?.displayName?.takeIf { it.isNotBlank() }
            PlacePhoto(name = name, attribution = author?.let { "Photo: $it" })
        }

    private fun quote(range: PriceRangeDto?): PriceQuote? {
        if (range == null) return null
        return PriceQuote(money(range.startPrice), money(range.endPrice))
    }

    private fun money(dto: MoneyDto?): Money? {
        if (dto == null) return null
        val units = dto.units.asLongOrNull() ?: return null
        return Money(
            currencyCode = dto.currencyCode.orEmpty(),
            units = units,
            nanos = dto.nanos ?: 0,
        )
    }

    private fun reviews(dto: PlaceDto): List<Review> =
        dto.reviews.orEmpty().map { review ->
            val text = review.text?.text?.takeIf { it.isNotBlank() }
                ?: review.originalText?.text.orEmpty()
            Review(
                authorName = review.authorAttribution?.displayName?.takeIf { it.isNotBlank() } ?: "Google user",
                authorPhotoUrl = review.authorAttribution?.photoUri,
                rating = review.rating,
                relativeTime = review.relativePublishTimeDescription,
                text = text,
                publishedAt = review.publishTime?.let { raw ->
                    runCatching { Instant.parse(raw) }.getOrNull()
                },
                mapsUri = review.googleMapsUri,
            )
        }
}
