package com.lhansqw.senaslsc.interpreter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlantillaPromptTest {

    @Test
    fun `la entrada incluye cada sena con su id y confianza`() {
        val texto = PlantillaPrompt.construirEntrada(
            listOf(sena("lsc.hola", confianza = 0.92f), sena("lsc.como", confianza = 0.581f, interrogativa = true)),
        )

        assertTrue(texto.contains("1. id=lsc.hola | glosa=HOLA | significado=hola | tipo=palabra | confianza=0.92"))
        assertTrue(texto.contains("2. id=lsc.como | glosa=COMO | significado=como | tipo=palabra | confianza=0.58 | interrogativa"))
    }

    @Test
    fun `la confianza para el modelo usa punto decimal y la del usuario coma`() {
        assertEquals("0,58", formatearConfianza(0.58f))
        assertTrue(PlantillaPrompt.construirEntrada(listOf(sena("lsc.hola", confianza = 0.5f))).contains("confianza=0.50"))
    }

    @Test
    fun `el contexto solo aparece si existe`() {
        val sin = PlantillaPrompt.construirEntrada(listOf(sena("lsc.hola")))
        val con = PlantillaPrompt.construirEntrada(listOf(sena("lsc.hola")), contexto = "Buenos días.")

        assertFalse(sin.contains("Frase anterior"))
        assertTrue(con.contains("Frase anterior (ya revisada por el usuario): \"Buenos días.\""))
    }

    @Test
    fun `las instrucciones incluyen el umbral y prohiben inventar senas`() {
        assertTrue(PlantillaPrompt.INSTRUCCIONES.contains("confianza menor que 0.75"))
        assertTrue(PlantillaPrompt.INSTRUCCIONES.contains("No añadas señas"))
        assertFalse(PlantillaPrompt.INSTRUCCIONES.contains("%UMBRAL%"))
    }

    @Test
    fun `una secuencia vacia no genera instrucciones`() {
        try {
            PlantillaPrompt.construirEntrada(emptyList())
            fail("Se esperaba IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
