package com.lhansqw.senaslsc.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ModeloTest {

    private fun entrada(id: String, tipo: TipoSena = TipoSena.PALABRA) =
        EntradaVocabulario(id = id, glosa = id.substringAfter('.').uppercase(), significado = id.substringAfter('.'), tipo = tipo)

    private fun esperarError(bloque: () -> Unit) {
        try {
            bloque()
            fail("Se esperaba IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `identificador debe llevar el prefijo de la lengua`() {
        esperarError { entrada("gracias") }
        esperarError { entrada("LSC.Gracias") }
        assertEquals("lsc", entrada("lsc.gracias").lengua)
    }

    @Test
    fun `vocabulario rechaza duplicados y senas de otra lengua`() {
        esperarError { Vocabulario("lsc", listOf(entrada("lsc.hola"), entrada("lsc.hola"))) }
        esperarError { Vocabulario("lsc", listOf(entrada("lsc.hola"), entrada("asl.hello"))) }
        esperarError { Vocabulario("lsc", emptyList()) }
    }

    @Test
    fun `vocabulario busca por identificador`() {
        val vocabulario = Vocabulario("lsc", listOf(entrada("lsc.hola"), entrada("lsc.gracias")))

        assertEquals(2, vocabulario.tamano)
        assertTrue("lsc.hola" in vocabulario)
        assertFalse("lsc.adios" in vocabulario)
        assertEquals("gracias", vocabulario.buscar("lsc.gracias")?.significado)
        assertNull(vocabulario.buscar("lsc.adios"))
    }

    @Test
    fun `mano debe tener 21 puntos`() {
        val punto = Punto3D(0.5f, 0.5f, 0f)
        esperarError { LandmarkFrame(0, List(20) { punto }, null, null, null) }

        val frame = LandmarkFrame(0, List(21) { punto }, null, null, null)
        assertTrue(frame.hayManos)
        assertFalse(LandmarkFrame(0, null, null, null, null).hayManos)
    }

    @Test
    fun `prediccion exige candidatas ordenadas y calcula el margen`() {
        esperarError {
            PrediccionSena(0, 100, listOf(Candidata("lsc.a", 0.2f), Candidata("lsc.b", 0.7f)), desconocida = false)
        }

        val prediccion = PrediccionSena(0, 100, listOf(Candidata("lsc.a", 0.7f), Candidata("lsc.b", 0.2f)), desconocida = false)
        assertEquals("lsc.a", prediccion.mejor?.id)
        assertEquals(0.5f, prediccion.margen, 1e-6f)
    }

    @Test
    fun `probabilidad y confianza deben estar entre 0 y 1`() {
        esperarError { Candidata("lsc.a", 1.2f) }
        esperarError { SenaConfirmada(entrada("lsc.a"), confianza = -0.1f, inicioMs = 0, finMs = 10) }
    }

    @Test
    fun `propuesta incierta sin motivo no es valida`() {
        esperarError {
            PropuestaFrase("Hola", listOf("lsc.hola"), incierta = true, motivo = null, preguntaAclaracion = null, origen = OrigenInterpretacion.REGLAS)
        }
    }
}
