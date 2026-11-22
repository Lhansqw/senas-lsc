package com.lhansqw.senaslsc.interpreter.remoto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparativaTest {

    /** Gemini simulado: usa todas las señas y marca como incierta la frase si alguna es dudosa. */
    private fun geminiCorrecto(i: String, mensaje: String): RespuestaGemini {
        val ids = Regex("id=(\\S+)").findAll(mensaje).map { it.groupValues[1] }.distinct().toList()
        val dudosa = Regex("confianza=0[.,]([0-9]+)").findAll(mensaje).any { "0.${it.groupValues[1]}".toFloat() < 0.6f }
        val motivo = if (dudosa) "\"Una seña tiene confianza baja.\"" else "null"
        val json = """{"propuesta":"Frase de prueba.","senas_usadas":[${ids.joinToString { "\"$it\"" }}],"incierta":$dudosa,"motivo":$motivo,"pregunta_aclaracion":null}"""
        return RespuestaGemini(json, tokensEntrada = 500, tokensSalida = 50, latenciaMs = 800)
    }

    @Test
    fun `los casos usan identificadores del vocabulario de LSC70`() {
        val ids = CASOS.flatMap { c -> c.senas.map { it.id } }.toSet()
        assertTrue(ids.containsAll(listOf("lsc.hola", "lsc.yo", "lsc.nombre", "lsc.anos", "lsc.dias", "lsc.n10", "lsc.a")))
        assertTrue(ids.all { Regex("^lsc\\.[a-z0-9_]+$").matches(it) })
    }

    @Test
    fun `informe con respuestas validas, latencia y coste`() {
        val resultados = comparar(CASOS, ::geminiCorrecto)
        val texto = informe("gemini-3.5-flash-lite", resultados)
        val consultados = resultados.count { it.caso.senas.size >= 2 }
        assertTrue(texto.contains("| Respuestas que cumplen el contrato | $consultados de $consultados |"))
        assertTrue(texto.contains("| Latencia p50 / p95 | 800 ms / 800 ms |"))
        assertTrue(texto.contains("| Tokens medios por frase (entrada / salida) | 500 / 50 |"))
        // 1000 × (500 × 0,30 + 50 × 2,50) / 1 000 000 = 0,275 USD
        assertTrue(texto, texto.contains("0,275 USD"))
        assertEquals(CASOS.size, texto.lines().count { it.startsWith("| ") && it.count { c -> c == '|' } == 7 } - 1)
    }

    @Test
    fun `ignorar una sena dudosa no cumple el contrato`() {
        val ignora = { _: String, m: String ->
            val ids = Regex("id=(\\S+)").findAll(m).map { it.groupValues[1] }.distinct().toList()
            RespuestaGemini("""{"propuesta":"Hola, soy Sol.","senas_usadas":[${ids.joinToString { "\"$it\"" }}],"incierta":false}""", 1, 1, 10)
        }
        val r = comparar(CASOS.filter { it.descripcion == "Seña dudosa" }, ignora).single()
        assertTrue(r.gemini is com.lhansqw.senaslsc.interpreter.ResultadoGenerativo.Descartada)
    }

    @Test
    fun `una respuesta que inventa senas no cumple el contrato`() {
        val inventa = { _: String, _: String ->
            RespuestaGemini("""{"propuesta":"Hola, quiero café.","senas_usadas":["lsc.hola","lsc.cafe"],"incierta":false}""", 1, 1, 10)
        }
        val r = comparar(CASOS.take(1), inventa).single()
        assertTrue(r.gemini is com.lhansqw.senaslsc.interpreter.ResultadoGenerativo.Descartada)
        assertTrue(informe("x", listOf(r)).contains("| Respuestas que cumplen el contrato | 0 de 1 |"))
    }

    @Test
    fun `los errores de la API se cuentan y no rompen la comparativa`() {
        val falla = { _: String, _: String -> throw ErrorGemini(ErrorGemini.Tipo.LIMITE_USO, "límite") }
        val r = comparar(CASOS.take(2), falla)
        assertTrue(r.all { it.error == "límite" })
        assertTrue(informe("x", r).contains("| Errores de la API | 2 |"))
    }
}
