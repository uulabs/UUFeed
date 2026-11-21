package au.com.uulabs.uufeed.domain

import java.time.Instant
import java.util.Locale

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

enum class PriceLevel(
    val apiValue: String,
    val symbol: String,
    val rank: Int,
) {
    FREE("PRICE_LEVEL_FREE", "Free", 0),
    INEXPENSIVE("PRICE_LEVEL_INEXPENSIVE", "$", 1),
    MODERATE("PRICE_LEVEL_MODERATE", "$$", 2),
    EXPENSIVE("PRICE_LEVEL_EXPENSIVE", "$$$", 3),
    VERY_EXPENSIVE("PRICE_LEVEL_VERY_EXPENSIVE", "$$$$", 4),
    ;

    companion object {
        fun fromApi(value: String?): PriceLevel? = entries.firstOrNull { it.apiValue == value }
    }
}

enum class Budget(
    val chip: String,
    val description: String,
    val max: PriceLevel?,
) {
    ANY("Any", "Any price", null),
    CHEAP("$", "Cheap eats", PriceLevel.INEXPENSIVE),
    MODERATE("$$", "Up to moderate", PriceLevel.MODERATE),
    PRICEY("$$$", "Up to pricey", PriceLevel.EXPENSIVE),
    SPLURGE("$$$$", "Fine dining welcome", PriceLevel.VERY_EXPENSIVE),
}

fun Budget.apiPriceLevels(): List<String>? {
    val cap = max ?: return null
    return PriceLevel.entries.filter { it.rank <= cap.rank }.map { it.apiValue }
}

data class SearchFilters(
    val radiusKm: Double,
    val minRating: Double?,
    val budget: Budget,
    val keyword: String,
) {
    fun textQuery(): String {
        val trimmed = keyword.trim()
        return if (trimmed.isEmpty()) "restaurants cafes" else trimmed
    }

    fun summary(anchor: String): String {
        val rating = when (minRating) {
            null -> "Any rating"
            else -> {
                val label = if (minRating % 1.0 == 0.0) minRating.toInt().toString() else minRating.toString()
                "$label+"
            }
        }
        val craving = keyword.trim().takeIf { it.isNotEmpty() }
        val base = "Within ${formatKm(radiusKm)} of $anchor · $rating · ${budget.description}"
        return if (craving == null) base else "$base · $craving"
    }
}

fun searchingWithin(radiusKm: Double, anchor: String): String =
    "Searching within ${formatKm(radiusKm)} of $anchor"

/**
 * GPS searches say "your location". A typed suburb uses the geocoded place,
 * such as "Wanniassa ACT 2903".
 */
fun searchAnchor(usingGps: Boolean, placeLabel: String, demoLabel: String? = null): String {
    if (demoLabel != null) return demoLabel.ifBlank { "Civic, Canberra" }
    if (usingGps) return "your location"
    return placeLabel.trim().ifBlank { "the place you typed" }
}

fun formatGeocodedPlace(
    subLocality: String?,
    locality: String?,
    adminArea: String?,
    postalCode: String?,
    addressLine: String? = null,
): String {
    val suburb = listOf(subLocality, locality).firstOrNull { !it.isNullOrBlank() }?.trim()
    val region = abbreviateRegion(adminArea)?.takeIf { it != suburb }
    val postcode = postalCode?.trim()?.takeIf { it.isNotEmpty() }
    val parts = listOfNotNull(suburb, region, postcode)
    if (parts.isNotEmpty()) return parts.joinToString(" ")
    return addressLine?.trim()?.takeIf { it.isNotEmpty() } ?: "the place you typed"
}

private fun abbreviateRegion(adminArea: String?): String? {
    val raw = adminArea?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return when (raw.lowercase()) {
        "australian capital territory" -> "ACT"
        "new south wales" -> "NSW"
        "victoria" -> "VIC"
        "queensland" -> "QLD"
        "south australia" -> "SA"
        "western australia" -> "WA"
        "tasmania" -> "TAS"
        "northern territory" -> "NT"
        else -> raw
    }
}

data class Money(
    val currencyCode: String,
    val units: Long,
    val nanos: Int,
) {
    fun format(): String {
        val amount = units + nanos / 1_000_000_000.0
        val number = if (nanos == 0) {
            units.toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
        val symbol = when (currencyCode) {
            "AUD" -> "A$"
            "USD" -> "US$"
            "EUR" -> "€"
            "GBP" -> "£"
            "NZD" -> "NZ$"
            else -> if (currencyCode.isBlank()) "" else "$currencyCode "
        }
        return "$symbol$number"
    }
}

data class PriceQuote(
    val start: Money?,
    val end: Money?,
)

fun formatPrice(level: PriceLevel?, quote: PriceQuote?): String {
    val range = quote?.let(::formatQuote)
    val symbol = level?.symbol
    return when {
        symbol != null && range != null -> "$symbol · $range"
        range != null -> range
        symbol != null -> symbol
        else -> "Price n/a"
    }
}

private fun formatQuote(quote: PriceQuote): String? {
    val start = quote.start ?: return null
    val end = quote.end
    return if (end != null && (end.units != start.units || end.nanos != start.nanos || end.currencyCode != start.currencyCode)) {
        "${start.format()}–${end.format()}"
    } else {
        start.format()
    }
}

data class PlacePhoto(
    val name: String,
    val attribution: String?,
) {
    fun mediaUrl(maxWidthPx: Int): String {
        if (name.startsWith("android.resource://") || name.startsWith("http://") || name.startsWith("https://")) {
            return name
        }
        if (name.startsWith("legacy:")) {
            val reference = name.removePrefix("legacy:")
            return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=$maxWidthPx&photo_reference=$reference"
        }
        return "https://places.googleapis.com/v1/${name.trimStart('/')}/media?maxWidthPx=$maxWidthPx"
    }
}

data class PlaceCandidate(
    val id: String,
    val name: String,
    val rating: Double?,
    val reviewCount: Int,
    val priceLevel: PriceLevel?,
    val priceLabel: String,
    val latitude: Double,
    val longitude: Double,
    val openNow: Boolean?,
    val primaryType: String?,
    val typeLabel: String?,
    val address: String?,
    val photos: List<PlacePhoto>,
    val businessStatus: String?,
)

data class Review(
    val authorName: String,
    val authorPhotoUrl: String?,
    val rating: Double?,
    val relativeTime: String?,
    val text: String,
    val publishedAt: Instant?,
    val mapsUri: String?,
)

fun PlaceDetails.filledFrom(summary: PlaceCandidate): PlaceDetails {
    val hasLocation = latitude != 0.0 || longitude != 0.0
    return copy(
        id = id.ifBlank { summary.id },
        name = name.ifBlank { summary.name },
        address = address ?: summary.address,
        rating = rating ?: summary.rating,
        reviewCount = if (reviewCount == 0) summary.reviewCount else reviewCount,
        priceLabel = if (priceLevel == null) summary.priceLabel else priceLabel,
        priceLevel = priceLevel ?: summary.priceLevel,
        openNow = openNow ?: summary.openNow,
        photos = photos.ifEmpty { summary.photos },
        typeLabel = typeLabel ?: summary.typeLabel,
        latitude = if (hasLocation) latitude else summary.latitude,
        longitude = if (hasLocation) longitude else summary.longitude,
    )
}

data class PlaceDetails(
    val id: String,
    val name: String,
    val address: String?,
    val phone: String?,
    val internationalPhone: String?,
    val website: String?,
    val mapsUri: String?,
    val rating: Double?,
    val reviewCount: Int,
    val priceLabel: String,
    val priceLevel: PriceLevel?,
    val openNow: Boolean?,
    val weekdayHours: List<String>,
    val photos: List<PlacePhoto>,
    val reviews: List<Review>,
    val editorial: String?,
    val typeLabel: String?,
    val latitude: Double,
    val longitude: Double,
)

data class SearchPage(
    val places: List<PlaceCandidate>,
    val nextPageToken: String?,
)

data class SearchOutcome(
    val places: List<PlaceCandidate>,
    val requestedRadiusKm: Double,
    val effectiveRadiusKm: Double,
    val exhausted: Boolean,
    val canSearchAgain: Boolean,
    val capped: Boolean,
)

data class PlacesQuery(
    val textQuery: String,
    val origin: GeoPoint,
    val radiusMeters: Double,
    val openNow: Boolean,
    val minRating: Double?,
    val priceLevels: List<String>?,
    val pageToken: String?,
    val rankPreference: String,
    val languageCode: String = "en",
    val regionCode: String = "AU",
)
