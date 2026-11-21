package com.uulabs.uufeed

import com.uulabs.uufeed.domain.maskPlacesKey
import com.uulabs.uufeed.domain.resolvePlacesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlacesKeyPolicyTest {
    @Test
    fun userKeyBeatsTheBuildTimeKey() {
        assertEquals("typed", resolvePlacesKey(" typed ", "baked"))
    }

    @Test
    fun blankUserKeyFallsBackToTheBuild() {
        assertEquals("baked", resolvePlacesKey("  ", "baked"))
        assertEquals("baked", resolvePlacesKey(null, "baked"))
    }

    @Test
    fun noKeyAtAll() {
        assertNull(resolvePlacesKey(null, " "))
        assertNull(resolvePlacesKey("", ""))
    }

    @Test
    fun maskShowsOnlyTheLastFour() {
        assertEquals("•••• abcd", maskPlacesKey("secret-abcd"))
        assertNull(maskPlacesKey("  "))
    }
}
