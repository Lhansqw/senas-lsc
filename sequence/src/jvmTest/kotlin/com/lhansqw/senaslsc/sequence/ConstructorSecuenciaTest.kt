package com.lhansqw.senaslsc.sequence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstructorSecuenciaTest {

    @Test
    fun `cierra la secuencia tras una pausa sin manos`() {
        val secuencia = ConstructorSecuencia(pausaCierreMs = 1_000)
        secuencia.agregar(confirmada("lsc.hola", 0, 900))
        secuencia.agregar(confirmada("lsc.gracias", 1_000, 2_000))

        assertFalse(secuencia.actualizar(2_500, hayManos = false))
        assertTrue(secuencia.actualizar(3_000, hayManos = false))
        assertTrue(secuencia.cerrada)
        assertEquals(listOf("lsc.hola", "lsc.gracias"), secuencia.actual.map { it.id })
    }

    @Test
    fun `las manos visibles aplazan el cierre`() {
        val secuencia = ConstructorSecuencia(pausaCierreMs = 1_000)
        secuencia.agregar(confirmada("lsc.hola", 0, 500))
        secuencia.actualizar(1_200, hayManos = true)

        assertFalse(secuencia.actualizar(2_000, hayManos = false))
        assertTrue(secuencia.actualizar(2_200, hayManos = false))
    }

    @Test
    fun `una secuencia vacia nunca se cierra`() {
        val secuencia = ConstructorSecuencia(pausaCierreMs = 1_000)
        assertFalse(secuencia.actualizar(10_000, hayManos = false))
        assertFalse(secuencia.cerrar())
    }

    @Test
    fun `una sena nueva tras el cierre empieza otra secuencia`() {
        val secuencia = ConstructorSecuencia(pausaCierreMs = 1_000)
        secuencia.agregar(confirmada("lsc.hola", 0, 500))
        secuencia.cerrar()
        secuencia.agregar(confirmada("lsc.gracias", 5_000, 5_500))

        assertFalse(secuencia.cerrada)
        assertEquals(listOf("lsc.gracias"), secuencia.actual.map { it.id })
    }

    @Test
    fun `se cierra al llegar al maximo de senas`() {
        val secuencia = ConstructorSecuencia(maximoSenas = 2)
        assertFalse(secuencia.agregar(confirmada("lsc.hola", 0, 500)))
        assertTrue(secuencia.agregar(confirmada("lsc.gracias", 600, 1_000)))
    }

    @Test
    fun `eliminar la ultima sena reabre la secuencia`() {
        val secuencia = ConstructorSecuencia()
        secuencia.agregar(confirmada("lsc.hola", 0, 500))
        secuencia.agregar(confirmada("lsc.gracias", 600, 1_000))
        secuencia.cerrar()

        assertEquals("lsc.gracias", secuencia.eliminarUltima()?.id)
        assertFalse(secuencia.cerrada)
        assertEquals(listOf("lsc.hola"), secuencia.actual.map { it.id })
    }
}
