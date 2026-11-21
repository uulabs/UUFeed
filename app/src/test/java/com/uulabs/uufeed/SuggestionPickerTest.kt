package com.uulabs.uufeed

import com.uulabs.uufeed.domain.SuggestionPicker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SuggestionPickerTest {
    private val picker = SuggestionPicker()

    @Test
    fun returnsEveryCandidateWhenThereAreFewerThanRequested() {
        val places = listOf(samplePlace("a", type = "cafe"), samplePlace("b", type = "bar"))
        val picked = picker.pick(places, 5, Random(1))
        assertEquals(setOf("a", "b"), picked.map { it.id }.toSet())
    }

    @Test
    fun spreadsAShortListAcrossTypes() {
        val cafes = (1..6).map { samplePlace("c$it", rating = 4.9, type = "cafe") }
        val others = listOf(
            samplePlace("thai", rating = 4.0, type = "thai_restaurant"),
            samplePlace("bar", rating = 4.0, type = "bar"),
        )
        val picked = picker.pick(cafes + others, 5, Random(3))
        assertEquals(5, picked.size)
        assertEquals(picked.size, picked.map { it.id }.toSet().size)
        assertTrue(picked.map { it.primaryType }.toSet().containsAll(setOf("cafe", "thai_restaurant", "bar")))
    }

    @Test
    fun doesNotAlwaysTakeTheHighestRatedInOrder() {
        val places = (0..7).map { index ->
            samplePlace(
                id = "r$index",
                rating = 4.2 + index * 0.1,
                reviews = 100,
                type = "cafe",
            )
        }
        val byRating = places.sortedByDescending { it.rating ?: 0.0 }.take(5).map { it.id }
        val orders = (0 until 40).map { seed ->
            picker.pick(places, 5, Random(seed)).map { it.id }
        }
        assertTrue(orders.all { it.size == 5 && it.toSet().size == 5 })
        assertTrue(orders.all { it.all { id -> id in places.map { place -> place.id } } })
        assertTrue(orders.distinct().size > 1)
        assertTrue(orders.any { it != byRating })
    }
}
