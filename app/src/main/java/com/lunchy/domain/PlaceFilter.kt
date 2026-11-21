package com.lunchy.domain

object PlaceFilter {
    /**
     * Client-side check on top of the Places query. The API is asked for places
     * that are open, but location bias is soft, so distance is enforced here.
     * A place with no openNow flag is kept: Text Search already applied openNow.
     * An explicit false, or a place with no price when a budget is set, is dropped.
     */
    fun matches(
        place: PlaceCandidate,
        filters: SearchFilters,
        origin: GeoPoint,
        radiusKm: Double = filters.radiusKm,
    ): Boolean {
        if (place.id.isBlank() || place.name.isBlank()) return false
        if (place.latitude !in -90.0..90.0 || place.longitude !in -180.0..180.0) return false
        if (place.openNow == false) return false
        val distance = Geo.distanceKm(origin, GeoPoint(place.latitude, place.longitude))
        if (distance > radiusKm + 0.02) return false
        val min = filters.minRating
        if (min != null && (place.rating == null || place.rating < min - 1e-9)) return false
        val cap = filters.budget.max
        if (cap != null) {
            val level = place.priceLevel ?: return false
            if (level.rank > cap.rank) return false
        }
        return true
    }
}
