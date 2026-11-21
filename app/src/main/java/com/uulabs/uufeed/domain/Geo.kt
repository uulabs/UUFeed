package com.uulabs.uufeed.domain

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val EARTH_KM = 6371.0088

    fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_KM * asin(sqrt(h).coerceAtMost(1.0))
    }
}

fun formatKm(km: Double): String {
    val rounded = (km * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        "${rounded.toInt()} km"
    } else {
        String.format(Locale.US, "%.1f km", rounded)
    }
}

fun formatDistanceKm(km: Double): String {
    val meters = km * 1000.0
    return if (meters < 950) {
        "${meters.roundToInt().coerceAtLeast(0)} m"
    } else {
        formatKm(km)
    }
}

fun formatRating(rating: Double?): String =
    if (rating == null) "No rating" else String.format(Locale.US, "%.1f", rating)

fun formatReviewCount(count: Int): String = when {
    count <= 0 -> "No reviews"
    count == 1 -> "1 review"
    else -> "$count reviews"
}
