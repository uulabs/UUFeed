package com.lunchy.domain

import kotlin.math.ln
import kotlin.random.Random

/**
 * Picks [count] places with a spread of types, weighted toward higher ratings,
 * instead of slicing the first rows the API returned.
 */
class SuggestionPicker {
    fun pick(
        candidates: List<PlaceCandidate>,
        count: Int,
        random: Random,
    ): List<PlaceCandidate> {
        if (count <= 0 || candidates.isEmpty()) return emptyList()
        if (candidates.size <= count) return candidates

        val buckets = candidates
            .groupBy { it.primaryType ?: "other" }
            .mapValues { (_, places) -> places.sortedByDescending { quality(it) }.toMutableList() }
            .toMutableMap()
        val typeOrder = buckets.keys.shuffled(random)
        val picked = mutableListOf<PlaceCandidate>()

        while (picked.size < count && buckets.values.any { it.isNotEmpty() }) {
            var took = false
            for (type in typeOrder) {
                val bucket = buckets[type] ?: continue
                if (bucket.isEmpty()) continue
                val choice = weightedDraw(bucket.take(4), random)
                bucket.removeAll { it.id == choice.id }
                picked += choice
                took = true
                if (picked.size == count) break
            }
            if (!took) break
        }
        return picked
    }

    private fun quality(place: PlaceCandidate): Double {
        val rating = place.rating ?: 3.8
        val reviews = ln(1.0 + place.reviewCount.coerceAtLeast(0))
        return rating * 2.0 + reviews * 0.45
    }

    private fun weightedDraw(list: List<PlaceCandidate>, random: Random): PlaceCandidate {
        if (list.size == 1) return list.first()
        val weights = list.map { quality(it).coerceAtLeast(0.25) }
        val total = weights.sum()
        var ticket = random.nextDouble(total)
        for (index in list.indices) {
            ticket -= weights[index]
            if (ticket <= 0.0) return list[index]
        }
        return list.last()
    }
}
