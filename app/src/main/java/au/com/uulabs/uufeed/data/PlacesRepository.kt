package au.com.uulabs.uufeed.data

import au.com.uulabs.uufeed.domain.EngineStep
import au.com.uulabs.uufeed.domain.PlaceDetails
import au.com.uulabs.uufeed.domain.PlacesQuery
import au.com.uulabs.uufeed.domain.SearchFilters
import au.com.uulabs.uufeed.domain.SearchOutcome
import au.com.uulabs.uufeed.domain.SuggestionSession
import au.com.uulabs.uufeed.domain.apiPriceLevels
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

class PlacesRepository(
    private val client: PlacesClient,
    private val session: SuggestionSession,
) {
    private val mutex = Mutex()

    suspend fun load(filters: SearchFilters, origin: au.com.uulabs.uufeed.domain.GeoPoint): SearchOutcome =
        guard {
            mutex.withLock {
                withContext(Dispatchers.IO) {
                    var step = session.next(filters, origin)
                    var fetches = 0
                    while (step is EngineStep.Fetch && fetches < MAX_FETCHES_PER_TAP) {
                        val page = client.search(step.toQuery(filters, origin))
                        fetches += 1
                        step = session.commit(page, step)
                    }
                    when (step) {
                        is EngineStep.Fetch -> session.takePartial()
                        is EngineStep.Done -> step.outcome
                    }
                }
            }
        }

    suspend fun details(placeId: String): PlaceDetails = guard {
        withContext(Dispatchers.IO) { client.details(placeId) }
    }

    private suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (places: PlacesException) {
        throw places
    } catch (io: IOException) {
        throw PlacesException("Couldn't reach Google Places. Check your connection and try again.")
    }

    companion object {
        const val MAX_FETCHES_PER_TAP = 4
    }
}

private fun EngineStep.Fetch.toQuery(filters: SearchFilters, origin: au.com.uulabs.uufeed.domain.GeoPoint): PlacesQuery =
    PlacesQuery(
        textQuery = filters.textQuery(),
        origin = origin,
        radiusMeters = (radiusKm * 1000.0).coerceIn(1.0, 50_000.0),
        openNow = true,
        minRating = filters.minRating,
        priceLevels = filters.budget.apiPriceLevels(),
        pageToken = pageToken,
        rankPreference = rankPreference,
    )
