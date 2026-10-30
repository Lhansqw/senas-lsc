package com.lhansqw.senaslsc.camara

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lhansqw.senaslsc.camera.AnalizadorFotogramas
import com.lhansqw.senaslsc.SenasApp
import com.lhansqw.senaslsc.camera.ControladorCamara
import com.lhansqw.senaslsc.grabacion.ControladorGrabacion
import com.lhansqw.senaslsc.historial.aOrigenTexto
import com.lhansqw.senaslsc.historial.glosasDe
import com.lhansqw.senaslsc.ia.ControladorIaLocal
import com.lhansqw.senaslsc.ia.ControladorIaRemota
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

    private val app = application as SenasApp

    val controlador = ControladorCamara()
    val landmarks = AnalizadorLandmarks(application)

    /** IA local: solo se carga si el teléfono es compatible, el modelo está instalado y el usuario la activó. */
    val ia = ControladorIaLocal(
        context = application,
        compatible = app.capacidad.interpreteLocal,
        motivoNoCompatible = app.capacidad.motivos.lastOrNull(),
        rutaModelo = app.gestorModelo.rutaInstalado,
        activadaPorUsuario = app.preferenciasIa.usarIaLocal.value,
    )
    /** IA de internet (Gemini): solo se usa con consentimiento y clave (fase 10). */
    private val remota = ControladorIaRemota(app.preferenciasIaRemota)
    val senas = ProcesadorSenas(application, ia, remota) { app.preferenciasIaRemota.preferencia.value }

    /** Herramienta de grabación de muestras para entrenar (fase 6). */
    val grabacion = ControladorGrabacion(application)

    /** Si el usuario activó el historial (desactivado por defecto). */
    val historialActivo: StateFlow<Boolean> = app.preferenciasHistorial.guardar

    private val _cargaSimuladaMs = MutableStateFlow(0L)

    /** Retardo artificial por fotograma para comprobar el descarte de fotogramas (solo pruebas). */
    val cargaSimuladaMs: StateFlow<Long> = _cargaSimuladaMs.asStateFlow()

    init {
        landmarks.alExtraer = { frame ->
            senas.enviarFotograma(frame)
            grabacion.recibir(frame)
        }
        controlador.analizador = AnalizadorFotogramas { imagen ->
            val analizado = landmarks.analizar(imagen)
            val retardo = _cargaSimuladaMs.value
            if (analizado && retardo > 0) Thread.sleep(retardo)
            analizado
        }
    }

    /**
     * Guarda la propuesta actual en el historial (solo si está activado). Devuelve `false` si no
     * había nada que guardar.
     */
    fun guardarEnHistorial(): Boolean {
        if (!historialActivo.value) return false
        val estado = senas.estado.value
        val propuesta = estado.propuesta ?: return false
        app.historial.guardar(propuesta.texto, glosasDe(propuesta, estado.senas), propuesta.origen.aOrigenTexto())
        return true
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
        ia.close()
        grabacion.close()
    }
}
