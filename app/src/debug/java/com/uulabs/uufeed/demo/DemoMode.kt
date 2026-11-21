package com.uulabs.uufeed.demo

import com.uulabs.uufeed.data.PlacesClient
import com.uulabs.uufeed.domain.GeoPoint

/**
 * Debug-only sample data. Not compiled into release builds.
 * Places, reviews, and pictures are invented for layout checks.
 */
object DemoMode {
    const val available: Boolean = true

    @Volatile
    var enabled: Boolean = false

    val origin: GeoPoint = GeoPoint(-35.2802, 149.1310)
}

fun demoPlacesClient(): PlacesClient? = DemoPlacesClient()
