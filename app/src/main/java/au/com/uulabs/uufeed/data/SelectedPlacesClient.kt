package au.com.uulabs.uufeed.data

import au.com.uulabs.uufeed.domain.PlaceDetails
import au.com.uulabs.uufeed.domain.PlacesQuery
import au.com.uulabs.uufeed.domain.SearchPage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Uses the backend saved with the key. If none is saved yet, probes once and
 * stores the result so later searches stay on the same API.
 */
class SelectedPlacesClient(
    private val modern: PlacesClient,
    private val legacy: PlacesClient,
    private val keys: PlacesKeyStore,
    private val tester: PlacesKeyTester,
) : PlacesClient {
    private val detectMutex = Mutex()

    override suspend fun search(query: PlacesQuery): SearchPage = client().search(query)

    override suspend fun details(placeId: String): PlaceDetails = client().details(placeId)

    private suspend fun client(): PlacesClient = when (backend()) {
        PlacesBackend.LEGACY -> legacy
        PlacesBackend.NEW -> modern
    }

    private suspend fun backend(): PlacesBackend {
        keys.readBackend()?.let { return it }
        return detectMutex.withLock {
            keys.readBackend()?.let { return@withLock it }
            val key = keys.resolve()
            if (key.isBlank()) throw MissingApiKeyException()
            when (val result = tester.test(key)) {
                is KeyTestResult.Success -> {
                    keys.saveBackend(result.backend)
                    result.backend
                }
                is KeyTestResult.Failed -> throw PlacesException(result.message)
            }
        }
    }
}
