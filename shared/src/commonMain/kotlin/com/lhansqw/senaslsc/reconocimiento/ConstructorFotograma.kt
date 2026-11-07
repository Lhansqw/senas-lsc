package com.lhansqw.senaslsc.reconocimiento

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.Punto3D
import com.lhansqw.senaslsc.vision.Subconjuntos

/**
 * Una mano detectada por el Hand Landmarker de MediaPipe (iOS).
 *
 * @property coordenadas 21 puntos seguidos como `x, y, z, x, y, z…` (63 valores), normalizados a la imagen.
 * @property izquierdaSegunEtiqueta lo que dice `handedness`, **ya corregido** a la mano izquierda de la
 *   persona. Solo se usa si no hay pose para decidir con las muñecas.
 */
class ManoDetectada(val coordenadas: FloatArray, val izquierdaSegunEtiqueta: Boolean)

/**
 * Convierte los resultados de MediaPipe de iOS en un [LandmarkFrame] igual al que produce Android.
 *
 * En Android se usa Holistic Landmarker, que ya dice qué mano es cuál. En iOS no hay Holistic, así
 * que se combinan Hand Landmarker (hasta 2 manos) y Pose Landmarker. Cada mano se asigna a la muñeca
 * de la pose más cercana (puntos 15 y 16, izquierda y derecha de la persona), que es como Holistic
 * recorta las manos. Si no hay pose, se usa la etiqueta de `handedness`.
 *
 * La imagen debe llegar **sin espejo**, como en Android (`ExtractorLandmarks`).
 */
object ConstructorFotograma {

    /** Índices de las muñecas en el Pose Landmarker completo (33 puntos). */
    private const val MUNECA_IZQUIERDA_POSE = 15
    private const val MUNECA_DERECHA_POSE = 16

    /**
     * @param pose 33 puntos seguidos `x, y, z…` (99 valores), o `null` si no se detectó el cuerpo.
     * @param relacionAspecto ancho / alto de la imagen analizada.
     */
    fun construir(
        timestampMs: Long,
        manos: List<ManoDetectada>,
        pose: FloatArray?,
        relacionAspecto: Float,
    ): LandmarkFrame {
        val puntosPose = pose?.aPuntos().orEmpty()
        val (izquierda, derecha) = asignarManos(manos.take(2), puntosPose)
        return Subconjuntos.construirFrame(
            timestampMs = timestampMs,
            manoIzquierda = izquierda?.coordenadas?.aPuntos().orEmpty(),
            manoDerecha = derecha?.coordenadas?.aPuntos().orEmpty(),
            pose = puntosPose,
            // El modelo actual no usa el rostro (usa_rostro: false), así que iOS no ejecuta Face Landmarker.
            rostro = emptyList(),
            relacionAspecto = relacionAspecto,
        )
    }

    /** Devuelve (mano izquierda, mano derecha) de la persona. */
    internal fun asignarManos(manos: List<ManoDetectada>, pose: List<Punto3D>): Pair<ManoDetectada?, ManoDetectada?> {
        if (manos.isEmpty()) return null to null
        if (pose.size < Subconjuntos.PUNTOS_POSE) return porEtiqueta(manos)

        val munecaIzq = pose[MUNECA_IZQUIERDA_POSE]
        val munecaDer = pose[MUNECA_DERECHA_POSE]
        if (manos.size == 1) {
            val mano = manos[0]
            val muneca = mano.muneca() ?: return porEtiqueta(manos)
            return if (distancia2(muneca, munecaIzq) <= distancia2(muneca, munecaDer)) mano to null else null to mano
        }
        val a = manos[0]
        val b = manos[1]
        val ma = a.muneca() ?: return porEtiqueta(manos)
        val mb = b.muneca() ?: return porEtiqueta(manos)
        // Se elige la asignación con menor distancia total: así dos manos nunca caen en el mismo lado.
        val directa = distancia2(ma, munecaIzq) + distancia2(mb, munecaDer)
        val cruzada = distancia2(mb, munecaIzq) + distancia2(ma, munecaDer)
        return if (directa <= cruzada) a to b else b to a
    }

    private fun porEtiqueta(manos: List<ManoDetectada>): Pair<ManoDetectada?, ManoDetectada?> {
        var izquierda = manos.firstOrNull { it.izquierdaSegunEtiqueta }
        var derecha = manos.firstOrNull { !it.izquierdaSegunEtiqueta }
        // Si las dos dicen lo mismo, la segunda va al otro lado para no perderla.
        if (manos.size == 2 && (izquierda == null || derecha == null)) {
            izquierda = manos[0].takeIf { it.izquierdaSegunEtiqueta } ?: manos[1]
            derecha = if (izquierda === manos[0]) manos[1] else manos[0]
        }
        return izquierda to derecha
    }

    private fun ManoDetectada.muneca(): Punto3D? =
        if (coordenadas.size >= 3) Punto3D(coordenadas[0], coordenadas[1], coordenadas[2]) else null

    /** Distancia al cuadrado en el plano de la imagen (z de mano y pose no son comparables). */
    private fun distancia2(a: Punto3D, b: Punto3D): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

    private fun FloatArray.aPuntos(): List<Punto3D> = List(size / 3) { i -> Punto3D(this[3 * i], this[3 * i + 1], this[3 * i + 2]) }
}
