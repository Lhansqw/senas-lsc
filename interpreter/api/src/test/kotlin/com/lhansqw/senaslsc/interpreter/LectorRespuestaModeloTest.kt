package com.lhansqw.senaslsc.interpreter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LectorRespuestaModeloTest {

    private fun correcta(texto: String) = (LectorRespuestaModelo.leer(texto) as LecturaRespuesta.Correcta).respuesta
    private fun motivo(texto: String) = (LectorRespuestaModelo.leer(texto) as LecturaRespuesta.Incorrecta).motivo

    @Test
    fun `lee una respuesta que es solo json`() {
        val r = correcta(
            """{"propuesta": "Me llamo Nik.", "senas_usadas": ["lsc.yo", "lsc.nombre"], "incierta": false, "motivo": null, "pregunta_aclaracion": null}""",
        )
        assertEquals("Me llamo Nik.", r.propuesta)
        assertEquals(listOf("lsc.yo", "lsc.nombre"), r.senasUsadas)
        assertNull(r.motivo)
    }

    @Test
    fun `tolera texto y bloques de codigo alrededor`() {
        val r = correcta(
            """
            Aquí tienes la respuesta:
            ```json
            {"propuesta": "¿Cómo estás?", "senas_usadas": ["lsc.como"], "incierta": true,
             "motivo": "COMO tuvo confianza baja", "pregunta_aclaracion": "¿Preguntas por la salud?"}
            ```
            """.trimIndent(),
        )
        assertTrue(r.incierta)
        assertEquals("¿Preguntas por la salud?", r.preguntaAclaracion)
    }

    @Test
    fun `los campos opcionales pueden faltar`() {
        val r = correcta("""{"propuesta": "Hola.", "senas_usadas": ["lsc.hola"], "incierta": false}""")
        assertNull(r.preguntaAclaracion)
    }

    @Test
    fun `informa por que no se puede leer`() {
        assertTrue(motivo("").contains("no respondió"))
        assertTrue(motivo("Hola, ¿en qué te ayudo?").contains("JSON"))
        assertTrue(motivo("""{"propuesta": "Hola."}""").contains("senas_usadas"))
        assertTrue(motivo("""{"propuesta": "Hola.", "senas_usadas": "lsc.hola", "incierta": false}""").contains("lista"))
        assertTrue(motivo("""{"propuesta": 3, "senas_usadas": [], "incierta": false}""").contains("propuesta"))
    }
}
