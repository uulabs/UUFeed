package com.lunchy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunchy.data.PlacesRepository
import com.lunchy.demo.DemoMode
import com.lunchy.domain.Budget
import com.lunchy.domain.GeoPoint
import com.lunchy.domain.PlaceCandidate
import com.lunchy.domain.PlaceDetails
import com.lunchy.domain.SearchFilters
import com.lunchy.domain.SearchNotices
import com.lunchy.location.DeviceLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class LocationMode { DEVICE, MANUAL }

enum class Phase { Idle, Searching, Results }

sealed interface DetailsState {
    data object Idle : DetailsState
    data class Loading(val summary: PlaceCandidate) : DetailsState
    data class Ready(val summary: PlaceCandidate, val details: PlaceDetails) : DetailsState
    data class Failed(val summary: PlaceCandidate, val message: String) : DetailsState
    data class Missing(val placeId: String) : DetailsState
}

data class LunchUiState(
    val locationMode: LocationMode = LocationMode.DEVICE,
    val permissionGranted: Boolean = false,
    val permissionAsked: Boolean = false,
    val locating: Boolean = false,
    val origin: GeoPoint? = null,
    val locationLabel: String = "",
    val manualQuery: String = "",
    val radiusKm: Float = 3f,
    val minRating: Double? = null,
    val budget: Budget = Budget.ANY,
    val keyword: String = "",
    val phase: Phase = Phase.Idle,
    val results: List<PlaceCandidate> = emptyList(),
    val notice: String? = null,
    val error: String? = null,
    val canSearchAgain: Boolean = false,
    val summary: String = "",
    val details: DetailsState = DetailsState.Idle,
    val demoEnabled: Boolean = false,
)

sealed interface LunchEvent {
    data object ShowResults : LunchEvent
}

class LunchViewModel(
    private val repository: PlacesRepository,
    private val location: DeviceLocation,
) : ViewModel() {
    private val _state = MutableStateFlow(LunchUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<LunchEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var searchJob: Job? = null
    private var detailsToken = 0
    private var committedFilters: SearchFilters? = null
    private var committedOrigin: GeoPoint? = null

    fun onPermissionResult(granted: Boolean, preferDevice: Boolean = false) {
        _state.update { current ->
            current.copy(
                permissionGranted = granted,
                permissionAsked = true,
                locationMode = when {
                    granted && preferDevice -> LocationMode.DEVICE
                    !granted && current.locationMode == LocationMode.DEVICE -> LocationMode.MANUAL
                    else -> current.locationMode
                },
            )
        }
        if (granted && _state.value.locationMode == LocationMode.DEVICE) {
            refreshDeviceLocation()
        }
    }

    fun setLocationMode(mode: LocationMode) {
        _state.update { it.copy(locationMode = mode, error = null) }
        if (mode == LocationMode.DEVICE && _state.value.permissionGranted && _state.value.origin == null) {
            refreshDeviceLocation()
        }
    }

    fun setManualQuery(value: String) {
        _state.update { it.copy(manualQuery = value, error = null) }
    }

    fun setRadius(km: Float) {
        _state.update { it.copy(radiusKm = km) }
    }

    fun setMinRating(rating: Double?) {
        _state.update { it.copy(minRating = rating) }
    }

    fun setBudget(budget: Budget) {
        _state.update { it.copy(budget = budget) }
    }

    fun setKeyword(value: String) {
        _state.update { it.copy(keyword = value) }
    }

    fun setDemoEnabled(enabled: Boolean) {
        if (!DemoMode.available) return
        DemoMode.enabled = enabled
        _state.update {
            it.copy(
                demoEnabled = enabled,
                origin = if (enabled) DemoMode.origin else it.origin,
                locationLabel = if (enabled) "Civic, Canberra" else it.locationLabel,
                error = null,
            )
        }
    }

    fun refreshDeviceLocation() {
        viewModelScope.launch {
            _state.update { it.copy(locating = true) }
            val fix = location.current()
            if (fix == null) {
                _state.update {
                    it.copy(
                        locating = false,
                        locationLabel = "",
                        error = "Couldn't get a location fix. Enter a suburb instead.",
                        locationMode = LocationMode.MANUAL,
                    )
                }
                return@launch
            }
            val label = location.reverse(fix) ?: "Current location"
            _state.update {
                it.copy(
                    locating = false,
                    origin = fix,
                    locationLabel = label,
                    locationMode = LocationMode.DEVICE,
                    error = if (it.error?.contains("location", ignoreCase = true) == true) null else it.error,
                )
            }
        }
    }

    fun find() = startSearch(again = false)

    fun searchAgain() = startSearch(again = true)

    fun openPlace(id: String) {
        val summary = _state.value.results.firstOrNull { it.id == id }
        if (summary == null) {
            _state.update { it.copy(details = DetailsState.Missing(id)) }
            return
        }
        val token = ++detailsToken
        _state.update { it.copy(details = DetailsState.Loading(summary)) }
        viewModelScope.launch {
            try {
                val details = repository.details(id)
                if (token != detailsToken) return@launch
                _state.update { it.copy(details = DetailsState.Ready(summary, details)) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (token != detailsToken) return@launch
                _state.update {
                    it.copy(
                        details = DetailsState.Failed(
                            summary,
                            error.message ?: "Couldn't load this place.",
                        ),
                    )
                }
            }
        }
    }

    private fun startSearch(again: Boolean) {
        if (searchJob?.isActive == true) return
        searchJob = viewModelScope.launch {
            val snapshot = _state.value
            _state.update { it.copy(phase = Phase.Searching, error = null) }
            if (!again) _events.emit(LunchEvent.ShowResults)
            val resolved = resolveQuery(again, snapshot) ?: return@launch
            val (filters, origin) = resolved
            committedFilters = filters
            committedOrigin = origin
            try {
                val outcome = repository.load(filters, origin)
                _state.update {
                    it.copy(
                        phase = Phase.Results,
                        origin = origin,
                        results = outcome.places,
                        notice = SearchNotices.message(outcome),
                        canSearchAgain = outcome.canSearchAgain,
                        summary = filters.summary(),
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        phase = if (it.results.isEmpty()) Phase.Idle else Phase.Results,
                        error = error.message ?: "Something went wrong.",
                    )
                }
            }
        }
    }

    private suspend fun resolveQuery(again: Boolean, snapshot: LunchUiState): Pair<SearchFilters, GeoPoint>? {
        if (again) {
            val filters = committedFilters
            val origin = committedOrigin
            if (filters != null && origin != null) return filters to origin
        }
        val filters = snapshot.toFilters()
        if (DemoMode.available && DemoMode.enabled) {
            _state.update { it.copy(origin = DemoMode.origin, locationLabel = "Civic, Canberra") }
            return filters to DemoMode.origin
        }
        val origin = if (snapshot.locationMode == LocationMode.MANUAL || !snapshot.permissionGranted) {
            val query = snapshot.manualQuery.trim()
            if (query.isEmpty()) {
                fail("Enter a suburb or address, or allow location.")
                return null
            }
            val labeled = location.geocode(query)
            if (labeled == null) {
                fail("Couldn't find \"$query\". Try a suburb and a city, like Braddon, Canberra.")
                return null
            }
            _state.update { it.copy(origin = labeled.point, locationLabel = labeled.label) }
            labeled.point
        } else {
            snapshot.origin ?: location.current()?.also { fix ->
                val label = location.reverse(fix) ?: "Current location"
                _state.update { it.copy(origin = fix, locationLabel = label, locating = false) }
            }
        }
        if (origin == null) {
            fail("Couldn't get a location fix. Enter a suburb instead.")
            _state.update { it.copy(locationMode = LocationMode.MANUAL) }
            return null
        }
        return filters to origin
    }

    private fun fail(message: String) {
        _state.update {
            it.copy(
                phase = if (it.results.isEmpty()) Phase.Idle else Phase.Results,
                error = message,
            )
        }
    }

    private fun LunchUiState.toFilters(): SearchFilters = SearchFilters(
        radiusKm = (radiusKm * 10f).roundToInt() / 10.0,
        minRating = minRating,
        budget = budget,
        keyword = keyword.trim(),
    )
}
