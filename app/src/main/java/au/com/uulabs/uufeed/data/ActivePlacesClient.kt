package au.com.uulabs.uufeed.data

import au.com.uulabs.uufeed.demo.DemoMode
import au.com.uulabs.uufeed.domain.PlaceDetails
import au.com.uulabs.uufeed.domain.PlacesQuery
import au.com.uulabs.uufeed.domain.SearchPage

/**
 * Debug builds can swap in [demo]. Release builds pass a null demo client and
 * [DemoMode.enabled] stays false, so this always calls Google.
 */
class ActivePlacesClient(
    private val live: PlacesClient,
    private val demo: PlacesClient?,
) : PlacesClient {
    override suspend fun search(query: PlacesQuery): SearchPage = active().search(query)

    override suspend fun details(placeId: String): PlaceDetails = active().details(placeId)

    private fun active(): PlacesClient =
        if (DemoMode.available && DemoMode.enabled && demo != null) demo else live
}
