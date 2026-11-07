package com.lhansqw.senaslsc.sequence

import com.lhansqw.senaslsc.core.model.LandmarkFrame

/**
 * Ventana deslizante definida por **tiempo**, no por número de fotogramas.
 *
 * La cámara entrega entre 6 y 30 FPS según el teléfono, la luz y la carga. Con una ventana de
 * fotogramas fija, la misma seña ocuparía duraciones distintas. Esta ventana guarda los
 * fotogramas de los últimos [duracionMs] y emite una cada [pasoMs]; después [Remuestreo] la
 * convierte en el número fijo de instantes que espera el modelo.
 *
 * @param minCobertura fracción mínima de [duracionMs] que deben cubrir los fotogramas para emitir
 *   (al empezar, o tras un hueco largo, la ventana aún está incompleta).
 * @param minProporcionManos fracción mínima de fotogramas con alguna mano; por debajo no se llama
 *   al modelo.
 */
class VentanaTemporal(
    private val duracionMs: Long = 1_000L,
    private val pasoMs: Long = 250L,
    private val minCobertura: Float = 0.8f,
    private val minProporcionManos: Float = 0.5f,
) {
    init {
        require(duracionMs > 0) { "La duración debe ser positiva" }
        require(pasoMs in 1..duracionMs) { "El paso debe estar entre 1 y la duración ($duracionMs ms)" }
        require(minCobertura in 0f..1f) { "minCobertura debe estar entre 0 y 1" }
        require(minProporcionManos in 0f..1f) { "minProporcionManos debe estar entre 0 y 1" }
    }

    private val fotogramas = ArrayDeque<LandmarkFrame>()
    private var ultimaEmisionMs: Long? = null

    fun agregar(frame: LandmarkFrame): List<LandmarkFrame>? {
        fotogramas.lastOrNull()?.let { ultimo ->
            require(frame.timestampMs >= ultimo.timestampMs) {
                "Fotograma fuera de orden: ${frame.timestampMs} < ${ultimo.timestampMs}"
            }
        }
        fotogramas.addLast(frame)
        while (frame.timestampMs - fotogramas.first().timestampMs > duracionMs) fotogramas.removeFirst()

        val cobertura = frame.timestampMs - fotogramas.first().timestampMs
        if (cobertura < minCobertura * duracionMs) return null

        val ultima = ultimaEmisionMs
        if (ultima != null && frame.timestampMs - ultima < pasoMs) return null
        ultimaEmisionMs = frame.timestampMs

        val conManos = fotogramas.count { it.hayManos }
        if (conManos < minProporcionManos * fotogramas.size) return null
        return fotogramas.toList()
    }

    fun reiniciar() {
        fotogramas.clear()
        ultimaEmisionMs = null
    }
}
