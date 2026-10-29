package com.lhansqw.senaslsc.camara

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lhansqw.senaslsc.camera.AnalizadorFotogramas
import com.lhansqw.senaslsc.camera.ControladorCamara
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Conserva la cámara y el modelo de MediaPipe al girar el teléfono: la pantalla se recrea, pero
 * los casos de uso de CameraX y el modelo ya cargado se reutilizan.
 */
class CamaraViewModel(application: Application) : AndroidViewModel(application) {

    val controlador = ControladorCamara()
    val landmarks = AnalizadorLandmarks(application)

    private val _cargaSimuladaMs = MutableStateFlow(0L)

    /** Retardo artificial por fotograma para comprobar el descarte de fotogramas (solo pruebas). */
    val cargaSimuladaMs: StateFlow<Long> = _cargaSimuladaMs.asStateFlow()

    init {
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
        if (!activar) landmarks.limpiar()
    }

    override fun onCleared() {
        controlador.analizador = null
        landmarks.close()
    }
}
