package com.lhansqw.senaslsc.camara

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lhansqw.senaslsc.camera.AnalizadorFotogramas
import com.lhansqw.senaslsc.camera.ControladorCamara
import com.lhansqw.senaslsc.reconocimiento.ProcesadorSenas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Conserva la cámara, MediaPipe y el reconocedor de señas al girar el teléfono: la pantalla se
 * recrea, pero los casos de uso de CameraX y los modelos ya cargados se reutilizan.
 *
 * Flujo: cámara → [AnalizadorLandmarks] (hilo de análisis) → [ProcesadorSenas] (hilo propio).
 */
class CamaraViewModel(application: Application) : AndroidViewModel(application) {

    val controlador = ControladorCamara()
    val landmarks = AnalizadorLandmarks(application)
    val senas = ProcesadorSenas(application)

    private val _cargaSimuladaMs = MutableStateFlow(0L)

    /** Retardo artificial por fotograma para comprobar el descarte de fotogramas (solo pruebas). */
    val cargaSimuladaMs: StateFlow<Long> = _cargaSimuladaMs.asStateFlow()

    init {
        landmarks.alExtraer = senas::enviarFotograma
        controlador.analizador = AnalizadorFotogramas { imagen ->
            landmarks.analizar(imagen)
            val retardo = _cargaSimuladaMs.value
            if (retardo > 0) Thread.sleep(retardo)
        }
    }

    fun establecerCargaSimulada(ms: Long) {
        _cargaSimuladaMs.value = ms.coerceIn(0, 1_000)
    }

    fun alternarAnalisis() {
        val activar = !controlador.analisisActivo.value
        controlador.establecerAnalisisActivo(activar)
        if (!activar) {
            landmarks.limpiar()
            // Al pausar se descarta la frase a medias: al reanudar se empieza de cero.
            senas.limpiar()
        }
    }

    override fun onCleared() {
        controlador.analizador = null
        landmarks.alExtraer = null
        landmarks.close()
        senas.close()
    }
}
