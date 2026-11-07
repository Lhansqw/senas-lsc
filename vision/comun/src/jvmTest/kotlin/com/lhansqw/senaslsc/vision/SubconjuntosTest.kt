package com.lhansqw.senaslsc.vision

import com.lhansqw.senaslsc.core.model.Punto3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubconjuntosTest {

    /** Puntos cuyo valor x es su propio índice, para comprobar qué se seleccionó. */
    private fun puntos(n: Int) = List(n) { Punto3D(it.toFloat(), 0f, 0f) }

    @Test
    fun `los indices no se repiten y estan dentro de rango`() {
        assertEquals(Subconjuntos.CUERPO.size, Subconjuntos.CUERPO.toSet().size)
        assertEquals(Subconjuntos.ROSTRO.size, Subconjuntos.ROSTRO.toSet().size)
        assertTrue(Subconjuntos.CUERPO.all { it in 0 until Subconjuntos.PUNTOS_POSE })
        assertTrue(Subconjuntos.ROSTRO.all { it in 0 until Subconjuntos.PUNTOS_ROSTRO_MINIMO })
    }

    @Test
    fun `las conexiones apuntan a puntos existentes`() {
        assertTrue(Subconjuntos.CONEXIONES_MANO.all { (a, b) -> a in 0..20 && b in 0..20 })
        assertTrue(Subconjuntos.CONEXIONES_CUERPO.all { (a, b) -> a in Subconjuntos.CUERPO.indices && b in Subconjuntos.CUERPO.indices })
        assertEquals(21, Subconjuntos.CONEXIONES_MANO.size)
    }

    @Test
    fun `selecciona los puntos del cuerpo y del rostro en el orden definido`() {
        val frame = Subconjuntos.construirFrame(
            timestampMs = 10,
            manoIzquierda = puntos(21),
            manoDerecha = emptyList(),
            pose = puntos(33),
            rostro = puntos(478),
        )

        assertEquals(Subconjuntos.CUERPO, frame.cuerpo!!.map { it.x.toInt() })
        assertEquals(Subconjuntos.ROSTRO, frame.rostro!!.map { it.x.toInt() })
        assertEquals(21, frame.manoIzquierda!!.size)
        assertNull(frame.manoDerecha)
    }

    @Test
    fun `lo que no se detecta queda en null`() {
        val frame = Subconjuntos.construirFrame(0, emptyList(), emptyList(), emptyList(), emptyList())

        assertNull(frame.manoIzquierda)
        assertNull(frame.manoDerecha)
        assertNull(frame.cuerpo)
        assertNull(frame.rostro)
    }

    @Test
    fun `listas incompletas se descartan en lugar de fallar`() {
        val frame = Subconjuntos.construirFrame(0, puntos(20), puntos(22), puntos(10), puntos(100))

        assertNull(frame.manoIzquierda)
        assertNull(frame.manoDerecha)
        assertNull(frame.cuerpo)
        assertNull(frame.rostro)
    }
}
