package com.lhansqw.senaslsc.camera

/**
 * Métricas del análisis de fotogramas que se muestran en pantalla.
 *
 * @property fps fotogramas analizados por segundo en el último segundo.
 * @property latenciaMediaMs tiempo medio que tarda el analizador por fotograma.
 * @property latenciaMaximaMs peor caso en la misma ventana.
 * @property analizados fotogramas analizados desde el último reinicio.
 */
data class Rendimiento(
    val fps: Float,
    val latenciaMediaMs: Float,
    val latenciaMaximaMs: Float,
    val analizados: Long,
) {
    companion object {
        val VACIO = Rendimiento(fps = 0f, latenciaMediaMs = 0f, latenciaMaximaMs = 0f, analizados = 0)
    }
}

/**
 * Calcula FPS y latencia sobre una ventana deslizante de tiempo.
 *
 * Usa la marca de tiempo del sensor de cada fotograma (no el reloj del sistema), así que mide el
 * ritmo real de fotogramas que llegan al análisis. Con la estrategia KEEP_ONLY_LATEST, si el
 * analizador es lento CameraX descarta fotogramas intermedios y los FPS de análisis bajan, pero
 * la vista previa sigue fluida.
 *
 * Kotlin puro y sin sincronización: se usa desde el único hilo de análisis.
 */
class MedidorRendimiento(private val ventanaNs: Long = 1_000_000_000L) {

    init {
        require(ventanaNs > 0) { "La ventana debe ser positiva" }
    }

    private val marcas = ArrayDeque<Long>()
    private val duraciones = ArrayDeque<Long>()
    private var analizados = 0L

    /**
     * @param timestampNs marca de tiempo del sensor del fotograma.
     * @param duracionNs lo que tardó el analizador con ese fotograma.
     */
    fun registrar(timestampNs: Long, duracionNs: Long) {
        require(duracionNs >= 0) { "La duración no puede ser negativa" }
        marcas.lastOrNull()?.let { ultima ->
            // Un salto atrás significa que la cámara se reinició: se empieza de cero.
            if (timestampNs < ultima) reiniciar()
        }
        marcas.addLast(timestampNs)
        duraciones.addLast(duracionNs)
        analizados++
        while (marcas.size > 1 && timestampNs - marcas.first() > ventanaNs) {
            marcas.removeFirst()
            duraciones.removeFirst()
        }
    }

    fun instantanea(): Rendimiento {
        if (marcas.isEmpty()) return Rendimiento.VACIO.copy(analizados = analizados)
        val intervalo = marcas.last() - marcas.first()
        val fps = if (marcas.size >= 2 && intervalo > 0) (marcas.size - 1) * 1e9f / intervalo else 0f
        return Rendimiento(
            fps = fps,
            latenciaMediaMs = (duraciones.average() / 1e6).toFloat(),
            latenciaMaximaMs = duraciones.max() / 1e6f,
            analizados = analizados,
        )
    }

    fun reiniciar() {
        marcas.clear()
        duraciones.clear()
        analizados = 0
    }
}
