package com.lhansqw.senaslsc.camara

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import com.lhansqw.senaslsc.camera.AnalizadorFotogramas
import com.lhansqw.senaslsc.camera.ReguladorAnalisis
import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.vision.ExtractorLandmarks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Último resultado de MediaPipe, con el tamaño de la imagen para poder dibujarlo. */
data class ResultadoLandmarks(
    val frame: LandmarkFrame,
    val anchoImagen: Int,
    val altoImagen: Int,
    val inferenciaMs: Long,
)

sealed interface EstadoModelo {
    data object SinCargar : EstadoModelo
    data object Listo : EstadoModelo
    data class Error(val mensaje: String) : EstadoModelo
}

/**
 * Une la cámara con MediaPipe: prepara cada fotograma y extrae sus puntos.
 *
 * El modelo se carga en el primer fotograma, en el hilo de análisis, para no bloquear la interfaz.
 * Todo el acceso a MediaPipe pasa por el mismo cerrojo, así que nunca se usa desde dos hilos a la
 * vez (al girar el teléfono el hilo de análisis cambia, pero el modelo se reutiliza). [close]
 * espera a que termine el fotograma en curso antes de liberar el modelo.
 *
 * Con [ReguladorAnalisis], si no se ven manos durante 2 s se analizan solo unos 5 fotogramas por
 * segundo hasta que vuelve a aparecer una mano (fase 11: ahorro de batería).
 */
class AnalizadorLandmarks(context: Context) : AnalizadorFotogramas, AutoCloseable {

    private val contextoApp = context.applicationContext
    private val bloqueo = Any()
    private val preparador = PreparadorImagen()
    private var extractor: ExtractorLandmarks? = null
    private var cerrado = false
    private val regulador = ReguladorAnalisis()

    private val _reposo = MutableStateFlow(false)

    /** Sin manos desde hace rato: se analizan menos fotogramas. */
    val reposo: StateFlow<Boolean> = _reposo.asStateFlow()

    private val _resultado = MutableStateFlow<ResultadoLandmarks?>(null)
    val resultado: StateFlow<ResultadoLandmarks?> = _resultado.asStateFlow()

    private val _estadoModelo = MutableStateFlow<EstadoModelo>(EstadoModelo.SinCargar)
    val estadoModelo: StateFlow<EstadoModelo> = _estadoModelo.asStateFlow()

    /** Recibe cada fotograma convertido en puntos (en el hilo de análisis); no debe bloquear. */
    @Volatile
    var alExtraer: ((LandmarkFrame) -> Unit)? = null

    override fun analizar(imagen: ImageProxy): Boolean {
        synchronized(bloqueo) {
            if (cerrado || _estadoModelo.value is EstadoModelo.Error) return false
            val marcaNs = imagen.imageInfo.timestamp
            if (!regulador.debeAnalizar(marcaNs)) return false
            val ext = extractor ?: cargarModelo() ?: return false

            val bitmap = preparador.preparar(imagen)
            val extraccion = ext.extraer(bitmap, marcaNs / 1_000_000)
            regulador.registrarResultado(marcaNs, extraccion?.frame?.hayManos == true)
            _reposo.value = regulador.enReposo
            if (extraccion == null) return true
            _resultado.value = ResultadoLandmarks(
                frame = extraccion.frame,
                anchoImagen = bitmap.width,
                altoImagen = bitmap.height,
                inferenciaMs = extraccion.inferenciaMs,
            )
            alExtraer?.invoke(extraccion.frame)
            return true
        }
    }

    /** Borra el último resultado (al pausar, para no dejar puntos congelados en pantalla). */
    fun limpiar() {
        _resultado.value = null
        synchronized(bloqueo) {
            regulador.reiniciar()
            _reposo.value = false
        }
    }

    override fun close() {
        synchronized(bloqueo) {
            cerrado = true
            extractor?.close()
            extractor = null
        }
    }

    private fun cargarModelo(): ExtractorLandmarks? = try {
        ExtractorLandmarks.crear(contextoApp).also {
            extractor = it
            _estadoModelo.value = EstadoModelo.Listo
        }
    } catch (e: Exception) {
        // Falta el asset, el archivo está dañado o el dispositivo no puede ejecutar el modelo.
        Log.e(TAG, "No se pudo cargar MediaPipe", e)
        _estadoModelo.value = EstadoModelo.Error(
            "No se pudo cargar el modelo de MediaPipe (${ExtractorLandmarks.MODELO}). " +
                "Compila de nuevo para descargarlo. Detalle: ${e.message ?: e.javaClass.simpleName}",
        )
        null
    }

    private companion object {
        const val TAG = "AnalizadorLandmarks"
    }
}
