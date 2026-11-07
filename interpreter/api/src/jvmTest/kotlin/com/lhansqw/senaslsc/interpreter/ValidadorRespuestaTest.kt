package com.lhansqw.senaslsc.interpreter

import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorRespuestaTest {

    private val validador = ValidadorRespuesta(umbralSenaDudosa = 0.75f)
    private val entrada = listOf(sena("lsc.yo"), sena("lsc.nombre"), sena("lsc.nik", significado = "Nik"))

    private fun respuesta(
        texto: String = "Me llamo Nik.",
        usadas: List<String> = listOf("lsc.yo", "lsc.nombre", "lsc.nik"),
        incierta: Boolean = false,
        motivo: String? = null,
    ) = RespuestaModelo(texto, usadas, incierta, motivo, preguntaAclaracion = null)

    private fun errores(resultado: ResultadoValidacion): List<String> =
        (resultado as? ResultadoValidacion.Invalida)?.errores ?: emptyList()

    @Test
    fun `acepta una respuesta que cumple el contrato`() {
        val resultado = validador.validar(entrada, respuesta(), OrigenInterpretacion.LOCAL)

        assertTrue(resultado is ResultadoValidacion.Valida)
        val propuesta = (resultado as ResultadoValidacion.Valida).propuesta
        assertEquals("Me llamo Nik.", propuesta.texto)
        assertEquals(OrigenInterpretacion.LOCAL, propuesta.origen)
    }

    @Test
    fun `rechaza senas inventadas`() {
        val resultado = validador.validar(
            entrada,
            respuesta(texto = "Me llamo Nik, mucho gusto.", usadas = listOf("lsc.yo", "lsc.nombre", "lsc.nik", "lsc.gusto")),
            OrigenInterpretacion.LOCAL,
        )
        assertTrue(errores(resultado).any { "lsc.gusto" in it })
    }

    @Test
    fun `rechaza usar una sena mas veces de las reconocidas`() {
        val resultado = validador.validar(
            entrada,
            respuesta(usadas = listOf("lsc.yo", "lsc.yo", "lsc.nombre", "lsc.nik")),
            OrigenInterpretacion.LOCAL,
        )
        assertTrue(errores(resultado).any { it.startsWith("Usa más veces") })
    }

    @Test
    fun `omitir una sena exige marcar la propuesta como incierta`() {
        val sinMarcar = validador.validar(entrada, respuesta(texto = "Nik.", usadas = listOf("lsc.nik")), OrigenInterpretacion.LOCAL)
        assertTrue(errores(sinMarcar).any { it.startsWith("Omite señas") })

        val marcada = validador.validar(
            entrada,
            respuesta(texto = "Nik.", usadas = listOf("lsc.nik"), incierta = true, motivo = "No encajan YO y NOMBRE."),
            OrigenInterpretacion.LOCAL,
        )
        assertTrue(marcada is ResultadoValidacion.Valida)
    }

    @Test
    fun `una sena de baja confianza exige marcar la propuesta como incierta`() {
        val conDudosa = listOf(sena("lsc.hola"), sena("lsc.como", confianza = 0.58f, interrogativa = true))
        val resultado = validador.validar(
            conDudosa,
            respuesta(texto = "Hola, ¿cómo estás?", usadas = listOf("lsc.hola", "lsc.como")),
            OrigenInterpretacion.LOCAL,
        )
        assertTrue(errores(resultado).any { "confianza baja" in it })
    }

    @Test
    fun `una propuesta incierta debe explicar el motivo`() {
        val resultado = validador.validar(entrada, respuesta(incierta = true, motivo = "  "), OrigenInterpretacion.LOCAL)
        assertTrue(errores(resultado).any { "motivo" in it })
    }

    @Test
    fun `rechaza propuestas vacias o demasiado largas`() {
        assertTrue(errores(validador.validar(entrada, respuesta(texto = "   "), OrigenInterpretacion.LOCAL)).isNotEmpty())
        assertTrue(errores(validador.validar(entrada, respuesta(texto = "a".repeat(301)), OrigenInterpretacion.LOCAL)).isNotEmpty())
    }

    @Test
    fun `una sola sena nunca pasa por el modelo de lenguaje`() {
        val resultado = validador.validar(
            listOf(sena("lsc.hola")),
            respuesta(texto = "Hola, ¿qué tal?", usadas = listOf("lsc.hola")),
            OrigenInterpretacion.LOCAL,
        )
        assertTrue(resultado is ResultadoValidacion.Invalida)
    }
}
