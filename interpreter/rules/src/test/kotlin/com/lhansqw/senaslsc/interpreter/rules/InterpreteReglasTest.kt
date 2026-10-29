package com.lhansqw.senaslsc.interpreter.rules

import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.core.model.TipoSena
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class InterpreteReglasTest {

    private val interprete = InterpreteReglas(umbralSenaDudosa = 0.75f)

    private fun palabra(id: String, significado: String, confianza: Float = 0.9f, interrogativa: Boolean = false) =
        SenaConfirmada(EntradaVocabulario(id, id.substringAfter('.').uppercase(), significado, TipoSena.PALABRA, interrogativa), confianza, 0, 100)

    private fun letra(l: Char, confianza: Float = 0.9f) =
        SenaConfirmada(EntradaVocabulario("lsc.${l.lowercaseChar()}", l.uppercase(), l.lowercase(), TipoSena.LETRA), confianza, 0, 100)

    @Test
    fun `una sola sena muestra su significado sin formar una oracion`() {
        val propuesta = interprete.componer(listOf(palabra("lsc.gracias", "gracias")))

        assertEquals("Gracias", propuesta.texto)
        assertFalse(propuesta.incierta)
        assertEquals(OrigenInterpretacion.REGLAS, propuesta.origen)
    }

    @Test
    fun `varias senas forman una frase con punto final`() {
        val propuesta = interprete.componer(listOf(palabra("lsc.buenos_dias", "buenos días"), palabra("lsc.gracias", "gracias")))
        assertEquals("Buenos días gracias.", propuesta.texto)
        assertEquals(listOf("lsc.buenos_dias", "lsc.gracias"), propuesta.senasUsadas)
    }

    @Test
    fun `las letras seguidas forman una palabra deletreada`() {
        val propuesta = interprete.componer(
            listOf(palabra("lsc.yo", "yo"), palabra("lsc.nombre", "nombre"), letra('N'), letra('I'), letra('K')),
        )
        assertEquals("Yo nombre NIK.", propuesta.texto)
        assertEquals(5, propuesta.senasUsadas.size)
    }

    @Test
    fun `un deletreo solo cuenta como una sena aislada`() {
        assertEquals("NIK", interprete.componer(listOf(letra('N'), letra('I'), letra('K'))).texto)
    }

    @Test
    fun `una sena interrogativa convierte la frase en pregunta`() {
        val propuesta = interprete.componer(listOf(palabra("lsc.tu", "tú"), palabra("lsc.como", "cómo", interrogativa = true)))
        assertEquals("¿Tú cómo?", propuesta.texto)
    }

    @Test
    fun `marca como incierta la propuesta con senas de baja confianza`() {
        val propuesta = interprete.componer(listOf(palabra("lsc.hola", "hola"), palabra("lsc.como", "cómo", confianza = 0.58f, interrogativa = true)))

        assertTrue(propuesta.incierta)
        assertEquals("Confianza baja en: COMO (0,58)", propuesta.motivo)
    }

    @Test
    fun `nunca anade senas ni pregunta de aclaracion`() {
        val secuencia = listOf(palabra("lsc.hola", "hola"), palabra("lsc.gracias", "gracias"))
        val propuesta = interprete.componer(secuencia)

        assertEquals(secuencia.map { it.id }, propuesta.senasUsadas)
        assertNull(propuesta.preguntaAclaracion)
    }

    @Test
    fun `una secuencia vacia es un error`() {
        try {
            interprete.componer(emptyList())
            fail("Se esperaba IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
