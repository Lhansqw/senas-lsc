package com.lhansqw.senaslsc.reconocimiento

import com.lhansqw.senaslsc.vision.Subconjuntos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ConstructorFotogramaTest {

    /** Mano de 21 puntos con la muñeca (punto 0) en (x, y) y el resto cerca. */
    private fun mano(x: Float, y: Float, izquierda: Boolean) =
        ManoDetectada(FloatArray(63) { i -> if (i % 3 == 0) x + i * 0.001f else if (i % 3 == 1) y else 0f }, izquierda)

    /** Pose de 33 puntos con la muñeca izquierda (15) en [izq] y la derecha (16) en [der]. */
    private fun pose(izq: Pair<Float, Float>, der: Pair<Float, Float>): FloatArray {
        val p = FloatArray(99) { 0.5f }
        p[15 * 3] = izq.first
        p[15 * 3 + 1] = izq.second
        p[16 * 3] = der.first
        p[16 * 3 + 1] = der.second
        return p
    }

    @Test
    fun `asigna cada mano a la muneca de la pose mas cercana aunque la etiqueta diga otra cosa`() {
        val cercaIzquierda = mano(0.8f, 0.5f, izquierda = false)
        val cercaDerecha = mano(0.2f, 0.5f, izquierda = true)
        val frame = ConstructorFotograma.construir(
            timestampMs = 10,
            manos = listOf(cercaDerecha, cercaIzquierda),
            pose = pose(izq = 0.8f to 0.5f, der = 0.2f to 0.5f),
            relacionAspecto = 0.75f,
        )
        assertEquals(0.8f, frame.manoIzquierda!![0].x, 1e-6f)
        assertEquals(0.2f, frame.manoDerecha!![0].x, 1e-6f)
        assertEquals(Subconjuntos.CUERPO.size, frame.cuerpo!!.size)
        assertNull(frame.rostro)
        assertEquals(0.75f, frame.relacionAspecto, 1e-6f)
    }

    @Test
    fun `una sola mano va al lado de su muneca`() {
        val frame = ConstructorFotograma.construir(0, listOf(mano(0.21f, 0.5f, izquierda = true)), pose(0.8f to 0.5f, 0.2f to 0.5f), 1f)
        assertNull(frame.manoIzquierda)
        assertNotNull(frame.manoDerecha)
    }

    @Test
    fun `sin pose usa la etiqueta y no pierde manos con etiquetas repetidas`() {
        val (izq, der) = ConstructorFotograma.asignarManos(listOf(mano(0.1f, 0f, true), mano(0.9f, 0f, false)), emptyList())
        assertEquals(0.1f, izq!!.coordenadas[0], 1e-6f)
        assertEquals(0.9f, der!!.coordenadas[0], 1e-6f)

        val (a, b) = ConstructorFotograma.asignarManos(listOf(mano(0.1f, 0f, true), mano(0.9f, 0f, true)), emptyList())
        assertNotNull(a)
        assertNotNull(b)
    }

    @Test
    fun `sin manos ni pose el fotograma queda vacio`() {
        val frame = ConstructorFotograma.construir(5, emptyList(), null, 1f)
        assertNull(frame.manoIzquierda)
        assertNull(frame.manoDerecha)
        assertNull(frame.cuerpo)
    }
}
