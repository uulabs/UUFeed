package au.com.uulabs.uufeed

import au.com.uulabs.uufeed.domain.Budget
import au.com.uulabs.uufeed.domain.SearchFilters
import au.com.uulabs.uufeed.domain.formatGeocodedPlace
import au.com.uulabs.uufeed.domain.searchAnchor
import au.com.uulabs.uufeed.domain.searchingWithin
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchPlaceLabelTest {
    @Test
    fun geocodedSuburbIncludesStateAndPostcode() {
        assertEquals(
            "Wanniassa ACT 2903",
            formatGeocodedPlace(
                subLocality = null,
                locality = "Wanniassa",
                adminArea = "Australian Capital Territory",
                postalCode = "2903",
            ),
        )
    }

    @Test
    fun suburbSearchNamesThePlaceAndGpsSaysYourLocation() {
        assertEquals(
            "Searching within 3 km of Wanniassa ACT 2903",
            searchingWithin(3.0, searchAnchor(usingGps = false, placeLabel = "Wanniassa ACT 2903")),
        )
        assertEquals(
            "Searching within 3 km of your location",
            searchingWithin(3.0, searchAnchor(usingGps = true, placeLabel = "Monash ACT 2904")),
        )
    }

    @Test
    fun resultsHeaderNamesTheSearchedPlace() {
        val filters = SearchFilters(radiusKm = 3.0, minRating = null, budget = Budget.ANY, keyword = "")
        assertEquals(
            "Within 3 km of Wanniassa ACT 2903 · Any rating · Any price",
            filters.summary("Wanniassa ACT 2903"),
        )
    }
}
