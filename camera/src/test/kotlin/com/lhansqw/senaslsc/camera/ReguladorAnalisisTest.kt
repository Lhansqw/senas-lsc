package com.lhansqw.senaslsc.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReguladorAnalisisTest {

    private val ms = 1_000_000L

    /** Simula una cámara a 30 FPS durante [duracionMs]; devuelve cuántos fotogramas se analizaron. */
    private fun simular(r: ReguladorAnalisis, desdeMs: Long, duracionMs: Long, manos: (Long) -> Boolean): Int {
        var analizados = 0
        var t = desdeMs
        while (t < desdeMs + duracionMs) {
            if (r.debeAnalizar(t * ms)) {
                r.registrarResultado(t * ms, manos(t))
                analizados++
            }
            t += 33
        }
        return analizados
    }

    @Test
    fun `con manos se analizan todos los fotogramas`() {
        val r = ReguladorAnalisis()
        assertEquals(31, simular(r, 0, 1_000) { true })
        assertFalse(r.enReposo)
    }

    @Test
    fun `sin manos pasa a reposo a los 2 s y analiza 5 por segundo`() {
        val r = ReguladorAnalisis()
        val primeros = simular(r, 0, 2_000) { false }
        assertEquals(61, primeros)               // los primeros 2 s, todos
        val enReposo = simular(r, 2_000, 2_000) { false }
        assertTrue(r.enReposo)
        assertTrue("analizados en reposo: $enReposo", enReposo in 9..11)   // ~5 FPS
    }

    @Test
    fun `una mano saca del reposo enseguida`() {
        val r = ReguladorAnalisis()
        simular(r, 0, 4_000) { false }
        assertTrue(r.enReposo)
        // La mano aparece a los 4,1 s: se ve en el siguiente fotograma analizado (≤ 200 ms).
        var vista: Long? = null
        var t = 4_000L
        while (vista == null) {
            if (r.debeAnalizar(t * ms)) {
                val hay = t >= 4_100
                r.registrarResultado(t * ms, hay)
                if (hay) vista = t
            }
            t += 33
        }
        assertTrue("vista a los $vista ms", vista!! - 4_100 <= 200)
        assertFalse(r.enReposo)
        assertEquals(31, simular(r, t, 1_000) { true })
    }

    @Test
    fun `tras dejar de ver manos espera 2 s antes de reposar`() {
        val r = ReguladorAnalisis()
        simular(r, 0, 1_000) { true }
        assertEquals(60, simular(r, 1_000, 1_980) { false })
        assertFalse(r.enReposo)
    }

    @Test
    fun `si la camara se reinicia se empieza de cero`() {
        val r = ReguladorAnalisis()
        simular(r, 10_000, 4_000) { false }
        assertTrue(r.enReposo)
        assertTrue(r.debeAnalizar(5 * ms))     // marca anterior: cámara nueva
        assertFalse(r.enReposo)
    }
}
