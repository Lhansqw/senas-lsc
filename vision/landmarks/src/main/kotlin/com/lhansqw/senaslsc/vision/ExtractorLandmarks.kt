package com.lhansqw.senaslsc.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarker
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarker.HolisticLandmarkerOptions
import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.Punto3D

/** Resultado de procesar un fotograma. */
data class Extraccion(
    val frame: LandmarkFrame,
    /** Tiempo de MediaPipe para este fotograma, en milisegundos. */
    val inferenciaMs: Long,
)

/**
 * Convierte imágenes en [LandmarkFrame] con MediaPipe Holistic Landmarker.
 *
 * Usa el modo VIDEO, que es síncrono: cada llamada devuelve el resultado de su fotograma. El
 * control de carga lo hace CameraX (KEEP_ONLY_LATEST): si MediaPipe tarda, los fotogramas
 * intermedios se descartan antes de llegar aquí.
 *
 * Crear el modelo tarda (cientos de milisegundos), por eso se hace una vez y se reutiliza.
 * No es seguro para varios hilos: se usa siempre desde el hilo de análisis.
 */
class ExtractorLandmarks private constructor(private val landmarker: HolisticLandmarker) : AutoCloseable {

    private var ultimoTimestampMs = Long.MIN_VALUE

    /**
     * Procesa una imagen ya girada en vertical (como la ve la cámara, **sin espejo**).
     *
     * @param timestampMs marca de tiempo del fotograma. En modo VIDEO debe crecer siempre; si no
     *   crece, el fotograma se ignora y se devuelve `null`.
     */
    fun extraer(imagen: Bitmap, timestampMs: Long): Extraccion? {
        if (timestampMs <= ultimoTimestampMs) return null
        ultimoTimestampMs = timestampMs

        val mpImage = BitmapImageBuilder(imagen).build()
        val inicio = SystemClock.elapsedRealtime()
        val resultado = landmarker.detectForVideo(mpImage, timestampMs)
        val inferenciaMs = SystemClock.elapsedRealtime() - inicio

        val frame = Subconjuntos.construirFrame(
            timestampMs = timestampMs,
            manoIzquierda = resultado.leftHandLandmarks().aPuntos(),
            manoDerecha = resultado.rightHandLandmarks().aPuntos(),
            pose = resultado.poseLandmarks().aPuntos(),
            rostro = resultado.faceLandmarks().aPuntos(),
            relacionAspecto = imagen.width.toFloat() / imagen.height,
        )
        return Extraccion(frame, inferenciaMs)
    }

    override fun close() = landmarker.close()

    companion object {
        /** Nombre del modelo en los assets; lo descarga la tarea de Gradle `descargarModeloHolistic`. */
        const val MODELO = "holistic_landmarker.task"

        /**
         * Crea el extractor. Lanza una excepción si el modelo no está en los assets o no se puede
         * cargar; quien llama debe mostrar el error.
         *
         * @param delegado CPU por defecto. La GPU se evalúa en la fase 11.
         */
        fun crear(
            context: Context,
            delegado: Delegate = Delegate.CPU,
            confianzaMinima: Float = 0.5f,
        ): ExtractorLandmarks {
            val base = BaseOptions.builder()
                .setModelAssetPath(MODELO)
                .setDelegate(delegado)
                .build()
            val opciones = HolisticLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setRunningMode(RunningMode.VIDEO)
                .setMinFaceDetectionConfidence(confianzaMinima)
                .setMinPoseDetectionConfidence(confianzaMinima)
                .setMinHandLandmarksConfidence(confianzaMinima)
                .setOutputFaceBlendshapes(false)
                .setOutputPoseSegmentationMasks(false)
                .build()
            return ExtractorLandmarks(HolisticLandmarker.createFromOptions(context.applicationContext, opciones))
        }

        private fun List<NormalizedLandmark>.aPuntos(): List<Punto3D> = map { Punto3D(it.x(), it.y(), it.z()) }
    }
}
