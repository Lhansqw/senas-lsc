package com.lhansqw.senaslsc.recognizer

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.Punto3D
import kotlin.math.sqrt

/**
 * Convierte los puntos de MediaPipe en el vector numérico que recibe el modelo.
 *
 * Es parte del **contrato del modelo**: el entrenamiento en Python (fase 6) debe calcular
 * exactamente los mismos valores, en el mismo orden. Si algo cambia aquí, sube [VERSION] y
 * reentrena.
 *
 * ## Sistema de referencia
 * - x se multiplica por la relación de aspecto, para que x e y estén en la misma unidad (el alto
 *   de la imagen).
 * - Origen: punto medio de los hombros. Escala: distancia entre hombros. Así la seña no depende
 *   de la distancia a la cámara ni de dónde esté la persona en la imagen.
 * - Si no se detecta el cuerpo: origen en el centro de la imagen y escala [ESCALA_SIN_CUERPO].
 *
 * ## Orden del vector de cada fotograma ([POR_FOTOGRAMA] = 352 valores)
 * 1. Mano izquierda ([POR_MANO] = 127): presencia (1/0); 21 × (x, y, z) respecto al cuerpo;
 *    21 × (x, y, z) respecto a la muñeca, escalados por el tamaño de la mano (forma).
 * 2. Mano derecha (127): igual.
 * 3. Cuerpo ([POR_CUERPO] = 15): presencia; 7 × (x, y) respecto al cuerpo.
 * 4. Rostro ([POR_ROSTRO] = 79): presencia; 39 × (x, y) respecto al cuerpo.
 * 5. Velocidad ([POR_VELOCIDAD] = 4): (vx, vy) de la muñeca izquierda y de la derecha, en
 *    unidades de referencia por segundo, respecto al fotograma anterior de la ventana.
 *
 * Todo lo que no se detecta vale 0, con su presencia a 0.
 */
object Caracteristicas {

    const val VERSION = 1

    /** Versión de `Subconjuntos` (módulo :vision:landmarks) con la que es compatible. */
    const val VERSION_SUBCONJUNTOS = 1

    const val PUNTOS_MANO = LandmarkFrame.PUNTOS_POR_MANO
    const val PUNTOS_CUERPO = 7
    const val PUNTOS_ROSTRO = 39

    const val POR_MANO = 1 + PUNTOS_MANO * 3 * 2
    const val POR_CUERPO = 1 + PUNTOS_CUERPO * 2
    const val POR_ROSTRO = 1 + PUNTOS_ROSTRO * 2
    const val POR_VELOCIDAD = 4
    const val POR_FOTOGRAMA = 2 * POR_MANO + POR_CUERPO + POR_ROSTRO + POR_VELOCIDAD

    /** Posiciones de los hombros dentro del subconjunto del cuerpo (nariz, hombro izq., hombro der., …). */
    private const val HOMBRO_IZQUIERDO = 1
    private const val HOMBRO_DERECHO = 2

    /** Punto de la mano usado para medir su tamaño: base del dedo medio. */
    private const val BASE_DEDO_MEDIO = 9

    const val ESCALA_SIN_CUERPO = 0.25f
    private const val MINIMO = 1e-6f

    /** Origen y escala del sistema de referencia de un fotograma. */
    data class Referencia(val origenX: Float, val origenY: Float, val escala: Float)

    fun referencia(frame: LandmarkFrame): Referencia {
        val ar = frame.relacionAspecto
        val cuerpo = frame.cuerpo?.takeIf { it.size == PUNTOS_CUERPO }
        if (cuerpo != null) {
            val izq = cuerpo[HOMBRO_IZQUIERDO]
            val der = cuerpo[HOMBRO_DERECHO]
            val distancia = distancia2D(izq.x * ar, izq.y, der.x * ar, der.y)
            if (distancia > MINIMO) {
                return Referencia((izq.x + der.x) / 2f * ar, (izq.y + der.y) / 2f, distancia)
            }
        }
        return Referencia(0.5f * ar, 0.5f, ESCALA_SIN_CUERPO)
    }

    /** Vector de una ventana completa: `ventana.size × POR_FOTOGRAMA` valores, fotograma a fotograma. */
    fun deVentana(ventana: List<LandmarkFrame>): FloatArray {
        val salida = FloatArray(ventana.size * POR_FOTOGRAMA)
        ventana.forEachIndexed { i, frame ->
            deFotograma(frame, ventana.getOrNull(i - 1), salida, i * POR_FOTOGRAMA)
        }
        return salida
    }

    /** Escribe las características de [frame] en [destino] a partir de la posición [desde]. */
    fun deFotograma(frame: LandmarkFrame, anterior: LandmarkFrame?, destino: FloatArray, desde: Int) {
        require(desde >= 0 && desde + POR_FOTOGRAMA <= destino.size) { "El destino no tiene espacio para un fotograma" }
        destino.fill(0f, desde, desde + POR_FOTOGRAMA)
        val ref = referencia(frame)
        val ar = frame.relacionAspecto
        var i = desde

        i = escribirMano(frame.manoIzquierda, ref, ar, destino, i)
        i = escribirMano(frame.manoDerecha, ref, ar, destino, i)
        i = escribirPuntos2D(frame.cuerpo?.takeIf { it.size == PUNTOS_CUERPO }, PUNTOS_CUERPO, ref, ar, destino, i)
        i = escribirPuntos2D(frame.rostro?.takeIf { it.size == PUNTOS_ROSTRO }, PUNTOS_ROSTRO, ref, ar, destino, i)

        if (anterior != null) {
            val dtSegundos = (frame.timestampMs - anterior.timestampMs) / 1000f
            if (dtSegundos > 0f) {
                val refAnterior = referencia(anterior)
                escribirVelocidad(frame.manoIzquierda, anterior.manoIzquierda, ref, refAnterior, ar, anterior.relacionAspecto, dtSegundos, destino, i)
                escribirVelocidad(frame.manoDerecha, anterior.manoDerecha, ref, refAnterior, ar, anterior.relacionAspecto, dtSegundos, destino, i + 2)
            }
        }
    }

    private fun escribirMano(mano: List<Punto3D>?, ref: Referencia, ar: Float, d: FloatArray, inicio: Int): Int {
        if (mano == null || mano.size != PUNTOS_MANO) return inicio + POR_MANO
        var i = inicio
        d[i++] = 1f
        for (p in mano) {
            d[i++] = (p.x * ar - ref.origenX) / ref.escala
            d[i++] = (p.y - ref.origenY) / ref.escala
            d[i++] = p.z * ar / ref.escala
        }
        val muneca = mano[0]
        val base = mano[BASE_DEDO_MEDIO]
        val tamano = distancia2D(muneca.x * ar, muneca.y, base.x * ar, base.y)
        if (tamano > MINIMO) {
            for (p in mano) {
                d[i++] = (p.x - muneca.x) * ar / tamano
                d[i++] = (p.y - muneca.y) / tamano
                d[i++] = (p.z - muneca.z) * ar / tamano
            }
        } else {
            i += PUNTOS_MANO * 3
        }
        return i
    }

    private fun escribirPuntos2D(puntos: List<Punto3D>?, cantidad: Int, ref: Referencia, ar: Float, d: FloatArray, inicio: Int): Int {
        if (puntos == null) return inicio + 1 + cantidad * 2
        var i = inicio
        d[i++] = 1f
        for (p in puntos) {
            d[i++] = (p.x * ar - ref.origenX) / ref.escala
            d[i++] = (p.y - ref.origenY) / ref.escala
        }
        return i
    }

    private fun escribirVelocidad(
        mano: List<Punto3D>?,
        manoAnterior: List<Punto3D>?,
        ref: Referencia,
        refAnterior: Referencia,
        ar: Float,
        arAnterior: Float,
        dtSegundos: Float,
        d: FloatArray,
        i: Int,
    ) {
        if (mano == null || manoAnterior == null) return
        val actualX = (mano[0].x * ar - ref.origenX) / ref.escala
        val actualY = (mano[0].y - ref.origenY) / ref.escala
        val previaX = (manoAnterior[0].x * arAnterior - refAnterior.origenX) / refAnterior.escala
        val previaY = (manoAnterior[0].y - refAnterior.origenY) / refAnterior.escala
        d[i] = (actualX - previaX) / dtSegundos
        d[i + 1] = (actualY - previaY) / dtSegundos
    }

    private fun distancia2D(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}
