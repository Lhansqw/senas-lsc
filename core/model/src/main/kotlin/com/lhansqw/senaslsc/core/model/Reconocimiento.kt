package com.lhansqw.senaslsc.core.model

/** Una posible seña con su probabilidad según el modelo visual. */
data class Candidata(val id: String, val probabilidad: Float) {
    init {
        require(probabilidad in 0f..1f) { "Probabilidad fuera de rango para $id: $probabilidad" }
    }
}

/**
 * Salida cruda del reconocedor para una ventana de fotogramas.
 * Todavía no es una seña confirmada: el filtro temporal decide si se acepta.
 */
data class PrediccionSena(
    val ventanaInicioMs: Long,
    val ventanaFinMs: Long,
    /** Ordenadas de mayor a menor probabilidad. */
    val candidatas: List<Candidata>,
    /** El modelo considera que la entrada no es ninguna seña de su vocabulario. */
    val desconocida: Boolean,
) {
    init {
        require(ventanaFinMs >= ventanaInicioMs) { "La ventana termina antes de empezar: $ventanaInicioMs–$ventanaFinMs" }
        require(candidatas.zipWithNext().all { (a, b) -> a.probabilidad >= b.probabilidad }) {
            "Las candidatas deben ir ordenadas de mayor a menor probabilidad"
        }
    }

    val mejor: Candidata? get() = candidatas.firstOrNull()

    /** Diferencia entre la primera y la segunda candidata: si es pequeña, la predicción es ambigua. */
    val margen: Float
        get() = when (candidatas.size) {
            0 -> 0f
            1 -> candidatas[0].probabilidad
            else -> candidatas[0].probabilidad - candidatas[1].probabilidad
        }
}

/**
 * Seña aceptada por el filtro temporal. Es lo único que llega al intérprete de texto:
 * el modelo de lenguaje nunca ve imágenes, puntos ni predicciones descartadas.
 */
data class SenaConfirmada(
    val entrada: EntradaVocabulario,
    /** Confianza media de las ventanas que la confirmaron, de 0 a 1. */
    val confianza: Float,
    val inicioMs: Long,
    val finMs: Long,
) {
    init {
        require(confianza in 0f..1f) { "Confianza fuera de rango: $confianza" }
        require(finMs >= inicioMs) { "La seña termina antes de empezar: $inicioMs–$finMs" }
    }

    val id: String get() = entrada.id
}
