package com.lhansqw.senaslsc.core.rendimiento

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EstadisticaLatenciaTest {

    @Test
    fun `sin mediciones no hay resumen`() {
        assertNull(EstadisticaLatencia().resumen())
        assertTrue(EstadisticaLatencia().vacia)
    }

    @Test
    fun `percentiles por rango mas cercano`() {
        val e = EstadisticaLatencia()
        (1..100).forEach { e.registrar(it.toFloat()) }
        val r = e.resumen()!!
        assertEquals(50f, r.p50Ms, 0f)
        assertEquals(95f, r.p95Ms, 0f)
        assertEquals(100f, r.maximaMs, 0f)
        assertEquals(100, r.muestras)
    }

    @Test
    fun `unos pocos tirones se ven en el p95 y no en el p50`() {
        val e = EstadisticaLatencia()
        repeat(90) { e.registrar(10f) }
        repeat(10) { e.registrar(80f) }
        val r = e.resumen()!!
        assertEquals(10f, r.p50Ms, 0f)
        assertEquals(80f, r.p95Ms, 0f)
    }

    @Test
    fun `solo recuerda las ultimas mediciones`() {
        val e = EstadisticaLatencia(capacidad = 10)
        repeat(10) { e.registrar(100f) }
        repeat(10) { e.registrar(1f) }
        assertEquals(1f, e.resumen()!!.maximaMs, 0f)
        assertEquals(10, e.resumen()!!.muestras)
    }

    @Test
    fun `nanosegundos a milisegundos y reinicio`() {
        val e = EstadisticaLatencia()
        e.registrarNanos(2_500_000L)
        assertEquals(2.5f, e.resumen()!!.p50Ms, 1e-6f)
        e.reiniciar()
        assertNull(e.resumen())
    }

    @Test
    fun `un solo valor es todos los percentiles`() {
        assertEquals(7f, EstadisticaLatencia.percentil(listOf(7f), 95.0), 0f)
        assertEquals(1f, EstadisticaLatencia.percentil(listOf(1f, 2f), 50.0), 0f)
    }

    @Test
    fun `rechaza latencias negativas`() {
        try {
            EstadisticaLatencia().registrar(-1f)
            fail("Debía rechazar una latencia negativa")
        } catch (_: IllegalArgumentException) {
        }
    }
}
