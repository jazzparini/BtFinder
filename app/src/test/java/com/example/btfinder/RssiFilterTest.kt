package com.example.btfinder

import com.example.btfinder.domain.RssiFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class RssiFilterTest {

    @Test
    fun `calcula promedio de RSSI`() {
        val filter = RssiFilter(capacity = 3)

        filter.add(-80)
        filter.add(-70)
        val result = filter.add(-60)

        assertEquals(-70, result)
    }

    @Test
    fun `descarta la muestra mas antigua al superar la capacidad`() {
        val filter = RssiFilter(capacity = 2)

        filter.add(-90)
        filter.add(-80)
        val result = filter.add(-60)

        // -90 ya fue descartado; el promedio es de (-80, -60).
        assertEquals(-70, result)
    }
}
