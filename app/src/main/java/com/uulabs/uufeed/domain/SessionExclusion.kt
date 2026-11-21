package com.uulabs.uufeed.domain

/**
 * Places already shown during this process. Not written to disk.
 * A new instance (a new process) starts empty.
 */
class SessionExclusion {
    private val shown = linkedSetOf<String>()

    val size: Int get() = shown.size

    fun shownIds(): Set<String> = shown.toSet()

    fun hasSeen(id: String): Boolean = id in shown

    fun markShown(ids: Iterable<String>) {
        shown += ids
    }

    fun filterUnseen(places: List<PlaceCandidate>): List<PlaceCandidate> =
        places.filter { it.id !in shown }
}
