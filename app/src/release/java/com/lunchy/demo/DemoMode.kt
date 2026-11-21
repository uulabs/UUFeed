package com.lunchy.demo

import com.lunchy.data.PlacesClient
import com.lunchy.domain.GeoPoint

/** Release builds have no demo catalogue and cannot enable one. */
object DemoMode {
    const val available: Boolean = false

    var enabled: Boolean
        get() = false
        set(_) = Unit

    val origin: GeoPoint = GeoPoint(-35.2802, 149.1310)
}

fun demoPlacesClient(): PlacesClient? = null
