package com.lhansqw.senaslsc.sequence

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import kotlin.math.abs

/**
 * Convierte una ventana con un número variable de fotogramas en [cantidad] instantes
 * equiespaciados en el tiempo. Es parte del contrato del modelo: el entrenamiento en Python
 * debe remuestrear igual.
 *
 * Regla: los instantes van de `fin − duración` a `fin` (ambos incluidos), donde `fin` es la marca
 * de tiempo del último fotograma. Para cada instante se toma el fotograma más cercano (en caso de
 * empate, el anterior) y se le asigna la marca de tiempo del instante.
 */
object Remuestreo {

    fun aTiempoUniforme(fotogramas: List<LandmarkFrame>, cantidad: Int, duracionMs: Long): List<LandmarkFrame> {
        require(fotogramas.isNotEmpty()) { "No hay fotogramas que remuestrear" }
        require(cantidad >= 2) { "Se necesitan al menos 2 instantes" }
        require(duracionMs > 0) { "La duración debe ser positiva" }

        val fin = fotogramas.last().timestampMs
        val inicio = fin - duracionMs
        var j = 0
        return List(cantidad) { i ->
            val instante = inicio + duracionMs * i / (cantidad - 1)
            // Avanza mientras el siguiente fotograma esté estrictamente más cerca del instante.
            while (j + 1 < fotogramas.size &&
                abs(fotogramas[j + 1].timestampMs - instante) < abs(fotogramas[j].timestampMs - instante)
            ) {
                j++
            }
            fotogramas[j].copy(timestampMs = maxOf(0L, instante))
        }
    }
}
