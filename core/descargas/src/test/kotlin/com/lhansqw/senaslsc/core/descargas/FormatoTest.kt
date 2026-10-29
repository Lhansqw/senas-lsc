package com.lhansqw.senaslsc.core.descargas

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoTest {

    @Test
    fun `tamanos en unidades binarias con coma decimal`() {
        assertEquals("2,5 GB", formatearTamano(2_708_000_000))
        assertEquals("700 MB", formatearTamano(734_003_200))
        assertEquals("2 KB", formatearTamano(2_048))
        assertEquals("512 B", formatearTamano(512))
        assertEquals("1,0 GB", formatearTamano(1L shl 30))
    }

    @Test
    fun `duraciones legibles`() {
        assertEquals("45 s", formatearDuracion(45))
        assertEquals("3 min 20 s", formatearDuracion(200))
        assertEquals("2 min", formatearDuracion(120))
        assertEquals("1 h 2 min", formatearDuracion(3_720))
        assertEquals("2 h", formatearDuracion(7_200))
        assertEquals("0 s", formatearDuracion(0))
    }

    @Test
    fun `velocidades legibles`() {
        assertEquals("1,5 MB/s", formatearVelocidad(1_572_864.0))
        assertEquals("300 KB/s", formatearVelocidad(307_200.0))
        assertEquals("10 B/s", formatearVelocidad(10.0))
    }
}
