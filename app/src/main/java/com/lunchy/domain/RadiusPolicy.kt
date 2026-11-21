package com.lunchy.domain

object RadiusPolicy {
    const val CAP_KM = 25.0
    const val MAX_WIDENS = 6
    const val MAX_PAGES_PER_RADIUS = 3

    private val ladder = doubleArrayOf(1.0, 2.0, 3.0, 5.0, 8.0, 12.0, 18.0, 25.0)

    fun widen(currentKm: Double, capKm: Double = CAP_KM): Double? {
        if (currentKm >= capKm - 0.05) return null
        val next = ladder.firstOrNull { it > currentKm + 0.05 }
        return when {
            next == null -> capKm
            next > capKm -> capKm
            else -> next
        }
    }
}
