package com.lhansqw.senaslsc.interpreter.litertlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ModelosLenguajeTest {

    @Test
    fun `el modelo por defecto se descarga por https y tiene extension litertlm`() {
        val m = ModelosLenguaje.POR_DEFECTO
        assertTrue(m.url.startsWith("https://"))
        assertTrue(m.archivo.endsWith(DescriptorModelo.EXTENSION))
        assertEquals("Apache 2.0", m.licencia)
    }

    @Test
    fun `un archivo demasiado pequeno no es el modelo`() {
        val m = ModelosLenguaje.GEMMA_4_E2B
        assertFalse(m.esArchivoPlausible(50L * 1024 * 1024))
        assertTrue(m.esArchivoPlausible(m.tamanoAproximado))
    }

    @Test
    fun `rechaza descriptores inseguros o incoherentes`() {
        val base = ModelosLenguaje.GEMMA_4_E2B
        listOf(
            { base.copy(url = "http://ejemplo.com/m.litertlm") },
            { base.copy(archivo = "modelo.bin") },
            { base.copy(tamanoMinimo = base.tamanoAproximado + 1) },
        ).forEach {
            try {
                it()
                fail("Se esperaba IllegalArgumentException")
            } catch (_: IllegalArgumentException) {
            }
        }
    }
}
