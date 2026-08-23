package com.example.btfinder

import com.example.btfinder.domain.Proximity
import com.example.btfinder.domain.isImprovement
import com.example.btfinder.domain.proximityFromRssi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximityTest {

    @Test
    fun `clasifica senal muy cercana`() {
        assertEquals(
            Proximity.VERY_CLOSE,
            proximityFromRssi(-50)
        )
    }

    @Test
    fun `clasifica senal cercana`() {
        assertEquals(
            Proximity.CLOSE,
            proximityFromRssi(-60)
        )
    }

    @Test
    fun `clasifica senal lejana`() {
        assertEquals(
            Proximity.FAR,
            proximityFromRssi(-80)
        )
    }

    @Test
    fun `clasifica senal debil`() {
        assertEquals(
            Proximity.WEAK,
            proximityFromRssi(-90)
        )
    }

    @Test
    fun `detecta mejora de senal de lejos a cerca`() {
        assertTrue(isImprovement(Proximity.FAR, Proximity.CLOSE))
    }

    @Test
    fun `no marca mejora si la senal empeora`() {
        assertFalse(isImprovement(Proximity.CLOSE, Proximity.FAR))
    }
}
