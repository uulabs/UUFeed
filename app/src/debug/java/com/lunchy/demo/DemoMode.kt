package com.lunchy.demo

import com.lunchy.data.PlacesClient
import com.lunchy.domain.GeoPoint

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
