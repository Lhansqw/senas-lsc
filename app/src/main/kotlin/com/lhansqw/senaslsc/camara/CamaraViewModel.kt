package com.lhansqw.senaslsc.camara

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.lhansqw.senaslsc.camera.AnalizadorFotogramas
import com.lhansqw.senaslsc.camera.ControladorCamara
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Conserva el controlador de cámara al girar el teléfono: la pantalla se recrea, pero los casos de
 * uso de CameraX se reutilizan y solo se vuelven a vincular al nuevo ciclo de vida.
 */
class CamaraViewModel : ViewModel() {

    val controlador = ControladorCamara()

    private val _cargaSimuladaMs = MutableStateFlow(0L)

    /** Retardo artificial por fotograma para comprobar que se descartan fotogramas (solo pruebas). */
    val cargaSimuladaMs: StateFlow<Long> = _cargaSimuladaMs.asStateFlow()

    init {
        // En la fase 5 este analizador se sustituye por MediaPipe.
        controlador.analizador = AnalizadorCargaSimulada { _cargaSimuladaMs.value }
    }

    fun establecerCargaSimulada(ms: Long) {
        _cargaSimuladaMs.value = ms.coerceIn(0, 1_000)
    }

    fun alternarAnalisis() {
        controlador.establecerAnalisisActivo(!controlador.analisisActivo.value)
    }
}

/**
 * Analizador provisional: no mira los píxeles, solo ocupa el hilo el tiempo indicado. Sirve para
 * ver que, con un análisis lento, los FPS de análisis bajan y la vista previa sigue fluida.
 */
private class AnalizadorCargaSimulada(private val retardoMs: () -> Long) : AnalizadorFotogramas {
    override fun analizar(imagen: ImageProxy) {
        val ms = retardoMs()
        if (ms > 0) Thread.sleep(ms)
    }
}
