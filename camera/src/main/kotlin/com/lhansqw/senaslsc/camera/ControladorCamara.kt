package com.lhansqw.senaslsc.camera

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Recibe cada fotograma de análisis en el hilo de análisis.
 *
 * El controlador cierra la imagen al volver de [analizar]: quien implemente esta interfaz no debe
 * guardar la [ImageProxy] ni usarla en otro hilo. Si necesita los píxeles más tarde (MediaPipe en
 * modo asíncrono, fase 5), debe copiarlos antes de volver.
 */
fun interface AnalizadorFotogramas {
    fun analizar(imagen: ImageProxy)
}

sealed interface EstadoCamara {
    data object Iniciando : EstadoCamara
    data class Activa(val frontal: Boolean, val resolucionAnalisis: Size?) : EstadoCamara
    data object SinCamara : EstadoCamara
    data class Error(val mensaje: String) : EstadoCamara
}

/**
 * Parámetros de captura.
 *
 * @property resolucionAnalisis resolución objetivo de los fotogramas de análisis. MediaPipe no
 *   necesita más: una resolución alta solo gasta CPU y batería. La vista previa usa la suya.
 */
data class ConfiguracionCamara(
    val resolucionAnalisis: Size = Size(640, 480),
    /** Cada cuánto se publican las métricas, en nanosegundos de sensor. */
    val intervaloMetricasNs: Long = 500_000_000L,
)

/**
 * Une CameraX con el resto de la app.
 *
 * - Vista previa: se expone como [surfaceRequest] para dibujarla con `CameraXViewfinder`.
 * - Análisis: `ImageAnalysis` con la estrategia `STRATEGY_KEEP_ONLY_LATEST`. Si el analizador va
 *   atrasado, CameraX descarta los fotogramas viejos en lugar de encolarlos: el reconocimiento
 *   trabaja siempre con lo más reciente y la latencia no crece.
 * - Las imágenes se cierran nada más analizarse: no se guarda ninguna.
 *
 * Se crea una vez (en un ViewModel) y se vincula con [vincular] cada vez que hay un
 * `LifecycleOwner` nuevo, por ejemplo tras girar el teléfono.
 */
class ControladorCamara(private val configuracion: ConfiguracionCamara = ConfiguracionCamara()) {

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    private val _estado = MutableStateFlow<EstadoCamara>(EstadoCamara.Iniciando)
    val estado: StateFlow<EstadoCamara> = _estado.asStateFlow()

    private val _rendimiento = MutableStateFlow(Rendimiento.VACIO)
    val rendimiento: StateFlow<Rendimiento> = _rendimiento.asStateFlow()

    private val _analisisActivo = MutableStateFlow(true)
    val analisisActivo: StateFlow<Boolean> = _analisisActivo.asStateFlow()

    /** Lo que se hace con cada fotograma. Se puede cambiar en caliente desde cualquier hilo. */
    @Volatile
    var analizador: AnalizadorFotogramas? = null

    // Solo se tocan desde el hilo de análisis.
    private val medidor = MedidorRendimiento()
    private var ultimaPublicacionNs = 0L

    // Vista previa y análisis en 4:3: así cubren el mismo campo de visión y los puntos de
    // MediaPipe se pueden dibujar exactamente encima de la imagen.
    private val vistaPrevia = Preview.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build(),
        )
        .build()
        .apply { setSurfaceProvider { solicitud -> _surfaceRequest.value = solicitud } }

    private val analisis = ImageAnalysis.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        configuracion.resolucionAnalisis,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    ),
                )
                .build(),
        )
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        // MediaPipe (fase 5) trabaja con mapas de bits RGBA: así se evita convertir desde YUV.
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .build()

    /**
     * Vincula la cámara al ciclo de vida y suspende hasta que se cancela la corrutina
     * (al salir de la pantalla). Entonces libera la cámara y el hilo de análisis.
     */
    suspend fun vincular(context: Context, lifecycleOwner: LifecycleOwner) {
        _estado.value = EstadoCamara.Iniciando
        val proveedor = ProcessCameraProvider.awaitInstance(context.applicationContext)

        // hasCamera lanza CameraInfoUnavailableException si no puede consultar la cámara.
        val tieneFrontal = runCatching { proveedor.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)
        val tieneTrasera = runCatching { proveedor.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)
        val selector = when {
            tieneFrontal -> CameraSelector.DEFAULT_FRONT_CAMERA
            tieneTrasera -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> {
                _estado.value = EstadoCamara.SinCamara
                return
            }
        }

        val hilo: ExecutorService = Executors.newSingleThreadExecutor { tarea ->
            Thread(tarea, "senas-analisis")
        }
        analisis.setAnalyzer(hilo, ::procesar)

        try {
            try {
                proveedor.unbindAll()
                proveedor.bindToLifecycle(lifecycleOwner, selector, vistaPrevia, analisis)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                fallo("No se pudo abrir la cámara", e)
                return
            } catch (e: IllegalArgumentException) {
                fallo("La cámara no admite la configuración pedida", e)
                return
            }

            _estado.value = EstadoCamara.Activa(
                frontal = selector == CameraSelector.DEFAULT_FRONT_CAMERA,
                resolucionAnalisis = analisis.resolutionInfo?.resolution,
            )
            awaitCancellation()
        } finally {
            analisis.clearAnalyzer()
            proveedor.unbind(vistaPrevia, analisis)
            // El reinicio va en la cola del hilo de análisis, detrás del último fotograma en curso,
            // para no tocar el medidor desde dos hilos a la vez.
            hilo.execute {
                medidor.reiniciar()
                ultimaPublicacionNs = 0L
            }
            hilo.shutdown()
            _surfaceRequest.value = null
            _rendimiento.value = Rendimiento.VACIO
        }
    }

    /** Pausa o reanuda el análisis. La vista previa sigue funcionando. */
    fun establecerAnalisisActivo(activo: Boolean) {
        _analisisActivo.value = activo
    }

    private fun procesar(imagen: ImageProxy) {
        imagen.use {
            if (!_analisisActivo.value) {
                if (_rendimiento.value != Rendimiento.VACIO) {
                    medidor.reiniciar()
                    _rendimiento.value = Rendimiento.VACIO
                }
                return
            }
            val inicio = System.nanoTime()
            try {
                analizador?.analizar(it)
            } catch (e: RuntimeException) {
                // Un fallo del analizador no debe tumbar la cámara; se registra y se sigue.
                Log.e(TAG, "Error en el analizador de fotogramas", e)
            }
            val marca: Long = it.imageInfo.timestamp
            medidor.registrar(marca, System.nanoTime() - inicio)

            if (marca - ultimaPublicacionNs >= configuracion.intervaloMetricasNs || marca < ultimaPublicacionNs) {
                ultimaPublicacionNs = marca
                _rendimiento.value = medidor.instantanea()
            }
        }
    }

    private fun fallo(mensaje: String, causa: Exception) {
        Log.e(TAG, mensaje, causa)
        _estado.value = EstadoCamara.Error(mensaje)
    }

    private companion object {
        const val TAG = "ControladorCamara"
    }
}
