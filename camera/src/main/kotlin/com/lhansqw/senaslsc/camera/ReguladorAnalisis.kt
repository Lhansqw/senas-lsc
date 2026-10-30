package com.lhansqw.senaslsc.camera

/**
 * Modo reposo (fase 11): si MediaPipe no ve manos durante [esperaReposoNs], se analiza como mucho
 * un fotograma cada [intervaloReposoNs] (5 FPS por defecto en vez de ~30). En cuanto vuelve a ver
 * una mano, se analizan todos otra vez.
 *
 * MediaPipe Holistic es, con diferencia, lo que más gasta por fotograma; con el teléfono delante y
 * nadie señando no tiene sentido ejecutarlo 30 veces por segundo. El primer fotograma con manos se
 * detecta como mucho [intervaloReposoNs] tarde, y la ventana de una seña dura 1 s.
 *
 * Usa la marca de tiempo del sensor. Kotlin puro y sin sincronización: usar desde un solo hilo.
 */
class ReguladorAnalisis(
    private val esperaReposoNs: Long = 2_000_000_000L,
    private val intervaloReposoNs: Long = 200_000_000L,
) {
    init {
        require(esperaReposoNs > 0) { "La espera debe ser positiva" }
        require(intervaloReposoNs > 0) { "El intervalo debe ser positivo" }
    }

    private var primerAnalisisNs: Long? = null
    private var ultimoAnalisisNs: Long? = null
    private var ultimaManoNs: Long? = null

    /** Verdadero si, en el último fotograma analizado, hacía rato que no se veían manos. */
    var enReposo: Boolean = false
        private set

    /** ¿Hay que analizar el fotograma con esta marca de tiempo? */
    fun debeAnalizar(timestampNs: Long): Boolean {
        ultimoAnalisisNs?.let { if (timestampNs < it) reiniciar() } // la cámara se reinició
        val referencia = ultimaManoNs ?: primerAnalisisNs ?: return true
        enReposo = timestampNs - referencia >= esperaReposoNs
        if (!enReposo) return true
        val ultimo = ultimoAnalisisNs ?: return true
        // Margen del 10 %: a 30 FPS los fotogramas llegan cada 33 ms y, sin él, 200 ms exactos
        // obligarían a esperar al de 231 ms (4,3 FPS en vez de 5).
        return timestampNs - ultimo >= intervaloReposoNs * 9 / 10
    }

    /** Informa del resultado de un fotograma analizado (también si MediaPipe no devolvió nada). */
    fun registrarResultado(timestampNs: Long, hayManos: Boolean) {
        if (primerAnalisisNs == null) primerAnalisisNs = timestampNs
        ultimoAnalisisNs = timestampNs
        if (hayManos) {
            ultimaManoNs = timestampNs
            enReposo = false
        }
    }

    fun reiniciar() {
        primerAnalisisNs = null
        ultimoAnalisisNs = null
        ultimaManoNs = null
        enReposo = false
    }
}
