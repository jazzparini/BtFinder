package com.example.btfinder.domain

/**
 * Clasificación de proximidad a partir del RSSI filtrado.
 * Ver RF-05 en bt_phone_instructions.md. Los valores son orientativos:
 * el RSSI no equivale directamente a una distancia en metros.
 */
enum class Proximity(
    val label: String,
    val emoji: String
) {
    VERY_CLOSE("Muy cerca", "🟢"),
    CLOSE("Cerca", "🟡"),
    FAR("Lejos", "🟠"),
    WEAK("Señal débil", "🔴"),
    NOT_FOUND("No detectado", "⚫")
}

fun proximityFromRssi(rssi: Int): Proximity {
    return when {
        rssi >= -55 -> Proximity.VERY_CLOSE
        rssi >= -70 -> Proximity.CLOSE
        rssi >= -85 -> Proximity.FAR
        else -> Proximity.WEAK
    }
}

/**
 * true si [next] representa una mejora de señal respecto a [previous]
 * (usado para decidir cuándo vibrar, RF opcional de la sección 2).
 */
fun isImprovement(previous: Proximity?, next: Proximity): Boolean {
    if (previous == null) return false
    val order = listOf(
        Proximity.NOT_FOUND,
        Proximity.WEAK,
        Proximity.FAR,
        Proximity.CLOSE,
        Proximity.VERY_CLOSE
    )
    return order.indexOf(next) > order.indexOf(previous)
}
