package com.lhansqw.senaslsc.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker.PoseLandmarkerOptions
import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.reconocimiento.ConstructorFotograma
import com.lhansqw.senaslsc.reconocimiento.ManoDetectada

/** Resultado de procesar un fotograma. */
data class Extraccion(
    val frame: LandmarkFrame,
    /** Tiempo de MediaPipe para este fotograma, en milisegundos. */
    val inferenciaMs: Long,
)

/**
 * Convierte imágenes en [LandmarkFrame] con MediaPipe Hand Landmarker (hasta 2 manos) y Pose
 * Landmarker, igual que la app de iPhone.
 *
 * Antes se usaba Holistic Landmarker, que calcula también la cara en cada fotograma. El modelo de
 * señas no la usa (`usa_rostro: false`) y en un teléfono de gama baja Holistic iba a ~4 FPS.
 * [ConstructorFotograma] (común con iOS) decide qué mano es cuál con las muñecas de la pose.
 * Medido con las personas de prueba de LSC70 (`training/paridad_manos_pose.py`): el modelo, entrenado
 * con Holistic, acierta el 87,2 % con estos puntos frente al 90,0 % con Holistic.
 *
 * Usa el modo VIDEO, que es síncrono: cada llamada devuelve el resultado de su fotograma. El
 * control de carga lo hace CameraX (KEEP_ONLY_LATEST): si MediaPipe tarda, los fotogramas
 * intermedios se descartan antes de llegar aquí.
 *
 * Crear los modelos tarda (cientos de milisegundos), por eso se hace una vez y se reutilizan.
 * No es seguro para varios hilos: se usa siempre desde el hilo de análisis.
 */
class ExtractorLandmarks private constructor(
    private val manos: HandLandmarker,
    private val pose: PoseLandmarker,
    /** Dónde corre MediaPipe; se muestra en el panel para comparar latencias. */
    val delegado: Delegate,
) : AutoCloseable {

    val enGpu: Boolean get() = delegado == Delegate.GPU

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
        val resultadoManos = manos.detectForVideo(mpImage, timestampMs)
        val resultadoPose = pose.detectForVideo(mpImage, timestampMs)
        val inferenciaMs = SystemClock.elapsedRealtime() - inicio

        val detectadas = resultadoManos.landmarks().mapIndexed { i, puntos ->
            // MediaPipe etiqueta las manos suponiendo una imagen en espejo. Esta llega sin espejo,
            // así que «Right» es la mano izquierda de la persona.
            val etiqueta = resultadoManos.handednesses().getOrNull(i)?.firstOrNull()?.categoryName()
            ManoDetectada(puntos.aCoordenadas(), izquierdaSegunEtiqueta = etiqueta == "Right")
        }
        val frame = ConstructorFotograma.construir(
            timestampMs = timestampMs,
            manos = detectadas,
            pose = resultadoPose.landmarks().firstOrNull()?.aCoordenadas(),
            relacionAspecto = imagen.width.toFloat() / imagen.height,
        )
        return Extraccion(frame, inferenciaMs)
    }

    override fun close() {
        manos.close()
        pose.close()
    }

    companion object {
        /** Modelos en los assets; los descargan las tareas de Gradle `descargarModeloManos` y `descargarModeloPose`. */
        const val MODELO_MANOS = "hand_landmarker.task"
        const val MODELO_POSE = "pose_landmarker_full.task"

        /**
         * Crea el extractor. Lanza una excepción si un modelo no está en los assets o no se puede
         * cargar; quien llama debe mostrar el error.
         *
         * @param delegado CPU por defecto; [crearPreferirGpu] prueba primero la GPU.
         */
        fun crear(
            context: Context,
            delegado: Delegate = Delegate.CPU,
            confianzaMinima: Float = 0.5f,
        ): ExtractorLandmarks {
            val app = context.applicationContext
            fun base(modelo: String) = BaseOptions.builder().setModelAssetPath(modelo).setDelegate(delegado).build()

            val opcionesManos = HandLandmarkerOptions.builder()
                .setBaseOptions(base(MODELO_MANOS))
                .setRunningMode(RunningMode.VIDEO)
                .setNumHands(2)
                .setMinHandDetectionConfidence(confianzaMinima)
                .setMinHandPresenceConfidence(confianzaMinima)
                .setMinTrackingConfidence(confianzaMinima)
                .build()
            val opcionesPose = PoseLandmarkerOptions.builder()
                .setBaseOptions(base(MODELO_POSE))
                .setRunningMode(RunningMode.VIDEO)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(confianzaMinima)
                .setMinPosePresenceConfidence(confianzaMinima)
                .setMinTrackingConfidence(confianzaMinima)
                .setOutputSegmentationMasks(false)
                .build()

            val manos = HandLandmarker.createFromOptions(app, opcionesManos)
            val pose = try {
                PoseLandmarker.createFromOptions(app, opcionesPose)
            } catch (e: Exception) {
                manos.close()
                throw e
            }
            return ExtractorLandmarks(manos, pose, delegado)
        }

        /**
         * Intenta la GPU (en teléfonos de gama baja suele ir bastante más rápida que la CPU) y, si
         * el teléfono no la admite, crea el extractor en CPU.
         */
        fun crearPreferirGpu(context: Context, confianzaMinima: Float = 0.5f): ExtractorLandmarks =
            try {
                crear(context, Delegate.GPU, confianzaMinima)
            } catch (e: Exception) {
                Log.w(TAG, "MediaPipe no pudo usar la GPU; se usa la CPU", e)
                crear(context, Delegate.CPU, confianzaMinima)
            }

        private const val TAG = "ExtractorLandmarks"

        /** Puntos seguidos como `x, y, z, x, y, z…`, el formato que espera [ConstructorFotograma]. */
        private fun List<NormalizedLandmark>.aCoordenadas(): FloatArray {
            val coordenadas = FloatArray(size * 3)
            forEachIndexed { i, p ->
                coordenadas[3 * i] = p.x()
                coordenadas[3 * i + 1] = p.y()
                coordenadas[3 * i + 2] = p.z()
            }
            return coordenadas
        }
    }
}
