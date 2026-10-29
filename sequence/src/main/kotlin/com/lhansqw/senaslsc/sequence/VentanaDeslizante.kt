package com.lhansqw.senaslsc.sequence

import com.lhansqw.senaslsc.core.model.LandmarkFrame

/**
 * Agrupa los fotogramas en ventanas solapadas para el reconocedor.
 *
 * Con `tamano = 30` y `paso = 10` (a 30 fps): cada ventana cubre 1 s y se emite una nueva
 * cada 0,33 s. Así el modelo no se ejecuta en cada fotograma.
 *
 * @param tamano fotogramas por ventana (los que espera el modelo).
 * @param paso fotogramas nuevos entre una ventana y la siguiente.
 * @param minProporcionManos fracción mínima de fotogramas con alguna mano. Por debajo, la ventana
 *   se descarta sin llamar al modelo: no hay seña posible y se ahorra batería.
 */
class VentanaDeslizante(
    private val tamano: Int,
    private val paso: Int,
    private val minProporcionManos: Float = 0.5f,
) {
    init {
        require(tamano > 0) { "El tamaño de la ventana debe ser positivo" }
        require(paso in 1..tamano) { "El paso debe estar entre 1 y el tamaño de la ventana ($tamano)" }
        require(minProporcionManos in 0f..1f) { "La proporción mínima de manos debe estar entre 0 y 1" }
    }

    private val buffer = ArrayDeque<LandmarkFrame>(tamano)
    private var nuevosDesdeUltima = 0
    private var primeraEmitida = false

    /**
     * Añade un fotograma. Devuelve una ventana cuando toca emitirla y tiene manos suficientes;
     * si no, `null`.
     */
    fun agregar(frame: LandmarkFrame): List<LandmarkFrame>? {
        buffer.lastOrNull()?.let { ultimo ->
            require(frame.timestampMs >= ultimo.timestampMs) {
                "Fotograma fuera de orden: ${frame.timestampMs} < ${ultimo.timestampMs}"
            }
        }
        if (buffer.size == tamano) buffer.removeFirst()
        buffer.addLast(frame)
        nuevosDesdeUltima++

        if (buffer.size < tamano) return null
        val toca = !primeraEmitida || nuevosDesdeUltima >= paso
        if (!toca) return null

        primeraEmitida = true
        nuevosDesdeUltima = 0

        val conManos = buffer.count { it.hayManos }
        if (conManos < minProporcionManos * tamano) return null
        return buffer.toList()
    }

    /** Vacía la ventana (al pausar, al cambiar de cámara o tras un salto de tiempo). */
    fun reiniciar() {
        buffer.clear()
        nuevosDesdeUltima = 0
        primeraEmitida = false
    }
}
