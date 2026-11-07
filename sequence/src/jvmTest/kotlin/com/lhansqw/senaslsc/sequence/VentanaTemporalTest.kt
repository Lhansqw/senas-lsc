package com.lhansqw.senaslsc.sequence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class VentanaTemporalTest {

    @Test
    fun `no emite hasta cubrir el 80 por ciento de la duracion`() {
        val ventana = VentanaTemporal(duracionMs = 1_000, pasoMs = 250)

        assertNull(ventana.agregar(frame(0)))
        assertNull(ventana.agregar(frame(500)))
        assertNull(ventana.agregar(frame(700)))
        assertNotNull(ventana.agregar(frame(800)))   // cubre 800 ms
    }

    @Test
    fun `emite una ventana cada paso y descarta los fotogramas viejos`() {
        val ventana = VentanaTemporal(duracionMs = 1_000, pasoMs = 250)
        val emitidas = (0..60).mapNotNull { i -> ventana.agregar(frame(i * 33L))?.let { i * 33L to it } }

        // Primera emisión en 825 ms (primer fotograma que cubre 800 ms) y después cada ≥ 250 ms.
        val tiempos = emitidas.map { it.first }
        assertEquals(825L, tiempos.first())
        tiempos.zipWithNext().forEach { (a, b) -> assertEquals(true, b - a >= 250) }
        // Ninguna ventana supera la duración.
        emitidas.forEach { (_, f) -> assertEquals(true, f.last().timestampMs - f.first().timestampMs <= 1_000) }
    }

    @Test
    fun `funciona igual a pocos fotogramas por segundo`() {
        // A 6 FPS (cada 166 ms) también se llena la ventana por tiempo.
        val ventana = VentanaTemporal(duracionMs = 1_000, pasoMs = 250)
        val emitida = (0..6).firstNotNullOfOrNull { ventana.agregar(frame(it * 166L)) }
        assertNotNull(emitida)
        assertEquals(6, emitida!!.size)  // 0…830 ms
    }

    @Test
    fun `sin manos suficientes no emite`() {
        val ventana = VentanaTemporal(duracionMs = 1_000, pasoMs = 100)
        (0..30).forEach { assertNull(ventana.agregar(frame(it * 33L, conManos = false))) }
    }

    @Test
    fun `reiniciar empieza de cero y el orden se valida`() {
        val ventana = VentanaTemporal(duracionMs = 1_000, pasoMs = 250)
        (0..30).forEach { ventana.agregar(frame(it * 33L)) }
        ventana.reiniciar()
        assertNull(ventana.agregar(frame(2_000)))

        try {
            ventana.agregar(frame(1_000))
            fail("Se esperaba IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
