package com.lunchy

import com.lunchy.domain.SessionExclusion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExclusionTest {
    @Test
    fun filterRemovesShownIdsAndKeepsTheRest() {
        val exclusion = SessionExclusion()
        val places = (1..4).map { samplePlace("p$it") }
        exclusion.markShown(listOf("p1", "p3"))
        assertEquals(listOf("p2", "p4"), exclusion.filterUnseen(places).map { it.id })
        exclusion.markShown(listOf("p2", "p2"))
        assertEquals(listOf("p4"), exclusion.filterUnseen(places).map { it.id })
        assertEquals(3, exclusion.size)
        assertTrue(exclusion.hasSeen("p1"))
        assertFalse(exclusion.hasSeen("p4"))
    }

    @Test
    fun aNewInstanceDoesNotRememberPreviousPlaces() {
        val first = SessionExclusion()
        first.markShown(listOf("p1"))
        assertEquals(0, SessionExclusion().size)
        assertTrue(SessionExclusion().filterUnseen(listOf(samplePlace("p1"))).isNotEmpty())
    }
}
