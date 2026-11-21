package au.com.uulabs.uufeed.demo

import au.com.uulabs.uufeed.data.PlacesClient
import au.com.uulabs.uufeed.domain.GeoPoint

/** Release builds have no demo catalogue and cannot enable one. */
object DemoMode {
    const val available: Boolean = false

    var enabled: Boolean
        get() = false
        set(_) = Unit

    val origin: GeoPoint = GeoPoint(-35.2802, 149.1310)
}

fun demoPlacesClient(): PlacesClient? = null
