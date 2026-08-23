package com.example.btfinder.domain

/**
 * Promedio móvil de las últimas [capacity] lecturas de RSSI, para evitar
 * cambios bruscos en el indicador de proximidad (RF-04).
 */
class RssiFilter(
    private val capacity: Int = 8
) {
    private val samples = ArrayDeque<Int>()

    fun add(value: Int): Int {
        if (samples.size >= capacity) {
            samples.removeFirst()
        }

        samples.addLast(value)
        return samples.average().toInt()
    }

    fun clear() {
        samples.clear()
    }
}
