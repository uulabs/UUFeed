package com.lunchy.domain

object SearchNotices {
    fun message(outcome: SearchOutcome): String? {
        val widened = outcome.effectiveRadiusKm > outcome.requestedRadiusKm + 0.05
        val requested = formatKm(outcome.requestedRadiusKm)
        val used = formatKm(outcome.effectiveRadiusKm)
        return when {
            outcome.places.isEmpty() && outcome.capped ->
                "Nothing new in this pass. Search again to look further out."
            outcome.places.isEmpty() ->
                "Nothing new is left within $used for these filters."
            outcome.capped && widened ->
                "Showing places within $used. Search again to keep looking."
            widened && outcome.exhausted ->
                "Looked out to $used. That's everything new that matches."
            widened ->
                "Nothing new within $requested. These are within $used."
            outcome.exhausted && outcome.places.size < 5 ->
                "Only ${outcome.places.size} new ${if (outcome.places.size == 1) "place" else "places"} left that match."
            outcome.exhausted ->
                "That's everything nearby that matches. Change the filters for a new pool."
            else -> null
        }
    }
}
