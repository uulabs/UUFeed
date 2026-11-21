package com.lunchy.domain

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

    fun summary(): String {
        val rating = when (minRating) {
            null -> "Any rating"
            else -> {
                val label = if (minRating % 1.0 == 0.0) minRating.toInt().toString() else minRating.toString()
                "$label+"
            }
        }
        val craving = keyword.trim().takeIf { it.isNotEmpty() }
        val base = "Within ${formatKm(radiusKm)} · $rating · ${budget.description}"
        return if (craving == null) base else "$base · $craving"
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
