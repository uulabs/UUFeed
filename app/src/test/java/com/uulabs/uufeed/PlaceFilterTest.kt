package com.uulabs.uufeed

import com.uulabs.uufeed.domain.Budget
import com.uulabs.uufeed.domain.PlaceFilter
import com.uulabs.uufeed.domain.PriceLevel
import com.uulabs.uufeed.domain.SearchFilters
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceFilterTest {
    private val filters = SearchFilters(
        radiusKm = 5.0,
        minRating = 4.0,
        budget = Budget.MODERATE,
        keyword = "",
    )

    @Test
    fun keepsAnOpenPlaceInsideTheRadius() {
        val place = samplePlace("ok", kmNorth = 1.0, rating = 4.2, price = PriceLevel.INEXPENSIVE)
        assertTrue(PlaceFilter.matches(place, filters, CANBERRA))
    }

    @Test
    fun dropsPlacesPastTheRadius() {
        val place = samplePlace("far", kmNorth = 8.0, rating = 4.8, price = PriceLevel.INEXPENSIVE)
        assertFalse(PlaceFilter.matches(place, filters, CANBERRA))
    }

    @Test
    fun includesTheBoundaryWithinTwentyMetres() {
        val place = samplePlace("edge", kmNorth = 5.0, rating = 4.0, price = PriceLevel.MODERATE)
        assertTrue(PlaceFilter.matches(place, filters, CANBERRA, radiusKm = 5.0))
    }

    @Test
    fun dropsClosedPlacesAndKeepsUnknownHours() {
        assertFalse(PlaceFilter.matches(samplePlace("shut", open = false), filters.copy(minRating = null, budget = Budget.ANY), CANBERRA))
        assertTrue(PlaceFilter.matches(samplePlace("maybe", open = null), filters.copy(minRating = null, budget = Budget.ANY), CANBERRA))
    }

    @Test
    fun ratingFloorIsInclusiveAndRejectsMissingRatings() {
        val anyBudget = filters.copy(budget = Budget.ANY)
        assertTrue(PlaceFilter.matches(samplePlace("exact", rating = 4.0), anyBudget, CANBERRA))
        assertFalse(PlaceFilter.matches(samplePlace("low", rating = 3.9), anyBudget, CANBERRA))
        assertFalse(PlaceFilter.matches(samplePlace("none", rating = null), anyBudget, CANBERRA))
        assertTrue(PlaceFilter.matches(samplePlace("none", rating = null), anyBudget.copy(minRating = null), CANBERRA))
    }

    @Test
    fun budgetIsACeilingAndHidesMissingPrices() {
        val cheap = filters.copy(minRating = null, budget = Budget.CHEAP)
        assertTrue(PlaceFilter.matches(samplePlace("free", price = PriceLevel.FREE), cheap, CANBERRA))
        assertTrue(PlaceFilter.matches(samplePlace("cheap", price = PriceLevel.INEXPENSIVE), cheap, CANBERRA))
        assertFalse(PlaceFilter.matches(samplePlace("mid", price = PriceLevel.MODERATE), cheap, CANBERRA))
        assertFalse(PlaceFilter.matches(samplePlace("unknown", price = null), cheap, CANBERRA))
        assertTrue(
            PlaceFilter.matches(
                samplePlace("unknown", price = null),
                cheap.copy(budget = Budget.ANY),
                CANBERRA,
            ),
        )
    }

    @Test
    fun usesKilometres() {
        val place = samplePlace("mile", kmNorth = 1.6, rating = 5.0, price = PriceLevel.FREE)
        assertFalse(PlaceFilter.matches(place, filters.copy(radiusKm = 1.5, minRating = null, budget = Budget.ANY), CANBERRA))
        assertTrue(PlaceFilter.matches(place, filters.copy(radiusKm = 1.8, minRating = null, budget = Budget.ANY), CANBERRA))
    }
}
