package com.uulabs.uufeed.domain

import kotlin.random.Random

sealed interface EngineStep {
    data class Fetch(
        val radiusKm: Double,
        val pageToken: String?,
        val rankPreference: String,
    ) : EngineStep

    data class Done(val outcome: SearchOutcome) : EngineStep
}

/**
 * In-memory search session. Shown place ids survive filter changes until the
 * process is gone. The fetched pool resets when the query changes. Paging state
 * is only committed after a successful response, so a failed request does not
 * widen the radius.
 */
class SuggestionSession(
    private val picker: SuggestionPicker = SuggestionPicker(),
    private val random: Random = Random.Default,
    private val desiredCount: Int = 5,
) {
    private val exclusion = SessionExclusion()
    private val pool = linkedMapOf<String, PlaceCandidate>()
    private var filters = SearchFilters(radiusKm = 3.0, minRating = null, budget = Budget.ANY, keyword = "")
    private var origin = GeoPoint(0.0, 0.0)
    private var started = false
    private var needsInitialFetch = true
    private var radiusKm = 3.0
    private var baseRadiusKm = 3.0
    private var pageToken: String? = null
    private var pagesThisRadius = 0
    private var widenSteps = 0

    fun shownIds(): Set<String> = exclusion.shownIds()

    fun next(filters: SearchFilters, origin: GeoPoint): EngineStep {
        val changed = !started || this.filters != filters || this.origin != origin
        if (changed) {
            this.filters = filters
            this.origin = origin
            pool.clear()
            pageToken = null
            pagesThisRadius = 0
            widenSteps = 0
            radiusKm = filters.radiusKm
            baseRadiusKm = filters.radiusKm
            needsInitialFetch = true
            started = true
        }
        return decide()
    }

    fun commit(page: SearchPage, requested: EngineStep.Fetch): EngineStep {
        val widening = requested.pageToken == null && requested.radiusKm > radiusKm + 0.05
        if (widening) {
            radiusKm = requested.radiusKm
            widenSteps += 1
            pagesThisRadius = 0
            pageToken = null
        }
        needsInitialFetch = false
        page.places.forEach { place ->
            if (place.id.isNotBlank()) pool[place.id] = place
        }
        pagesThisRadius += 1
        pageToken = page.nextPageToken
        return decide()
    }

    fun takePartial(): SearchOutcome {
        val picked = pickFrom(unseenMatching())
        return outcome(picked, capped = true, canSearchAgain = hasMore())
    }

    private fun decide(): EngineStep {
        if (needsInitialFetch) {
            return EngineStep.Fetch(radiusKm, null, rankFor(radiusKm))
        }
        val matching = unseenMatching()
        if (matching.size >= desiredCount) {
            val picked = picker.pick(matching, desiredCount, random)
            exclusion.markShown(picked.map { it.id })
            return EngineStep.Done(outcome(picked, capped = false, canSearchAgain = hasMore()))
        }
        if (pageToken != null && pagesThisRadius < RadiusPolicy.MAX_PAGES_PER_RADIUS) {
            return EngineStep.Fetch(radiusKm, pageToken, rankFor(radiusKm))
        }
        if (widenSteps < RadiusPolicy.MAX_WIDENS) {
            val wider = RadiusPolicy.widen(radiusKm)
            if (wider != null) {
                return EngineStep.Fetch(wider, null, "DISTANCE")
            }
        }
        val picked = picker.pick(matching, desiredCount, random)
        exclusion.markShown(picked.map { it.id })
        return EngineStep.Done(outcome(picked, capped = false, canSearchAgain = hasMore()))
    }

    private fun pickFrom(matching: List<PlaceCandidate>): List<PlaceCandidate> {
        val count = desiredCount.coerceAtMost(matching.size)
        val picked = picker.pick(matching, count, random)
        exclusion.markShown(picked.map { it.id })
        return picked
    }

    private fun unseenMatching(): List<PlaceCandidate> =
        exclusion.filterUnseen(pool.values.toList()).filter { place ->
            PlaceFilter.matches(place, filters, origin, radiusKm)
        }

    private fun hasMore(): Boolean {
        val remaining = unseenMatching().size
        if (remaining > 0) return true
        if (pageToken != null && pagesThisRadius < RadiusPolicy.MAX_PAGES_PER_RADIUS) return true
        if (widenSteps < RadiusPolicy.MAX_WIDENS && RadiusPolicy.widen(radiusKm) != null) return true
        return false
    }

    private fun rankFor(radius: Double): String =
        if (radius <= baseRadiusKm + 0.05) "RELEVANCE" else "DISTANCE"

    private fun outcome(
        picked: List<PlaceCandidate>,
        capped: Boolean,
        canSearchAgain: Boolean,
    ): SearchOutcome = SearchOutcome(
        places = picked,
        requestedRadiusKm = baseRadiusKm,
        effectiveRadiusKm = radiusKm,
        exhausted = !canSearchAgain,
        canSearchAgain = canSearchAgain,
        capped = capped,
    )
}
