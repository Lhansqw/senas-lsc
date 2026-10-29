package com.lhansqw.senaslsc.interpreter

import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class InterpreteGenerativoTest {

    private fun <T> ejecutar(bloque: suspend () -> T): T {
        var resultado: Result<T>? = null
        bloque.startCoroutine(Continuation(EmptyCoroutineContext) { resultado = it })
        return resultado!!.getOrThrow()
    }

    /** Respaldo reconocible: une los ids. */
    private val respaldo = object : Interprete {
        override val origen = OrigenInterpretacion.REGLAS
        override suspend fun proponer(secuencia: List<SenaConfirmada>, contexto: String?) =
            PropuestaFrase(secuencia.joinToString(" ") { it.id }, secuencia.map { it.id }, false, null, null, origen)
    }

    private val secuencia = listOf(sena("lsc.yo"), sena("lsc.nombre"), sena("lsc.nik", significado = "Nik"))

    private fun interprete(respuesta: suspend (String, String) -> String) =
        InterpreteGenerativo(GeneradorTexto { i, m -> respuesta(i, m) }, OrigenInterpretacion.LOCAL, respaldo)

    @Test
    fun `acepta una respuesta valida del modelo`() {
        val i = interprete { _, _ ->
            """{"propuesta": "Me llamo Nik.", "senas_usadas": ["lsc.yo", "lsc.nombre", "lsc.nik"], "incierta": false}"""
        }
        val r = ejecutar { i.interpretar(secuencia) }

        assertTrue(r is ResultadoGenerativo.Aceptada)
        assertEquals("Me llamo Nik.", r.propuesta.texto)
        assertEquals(OrigenInterpretacion.LOCAL, r.propuesta.origen)
    }

    @Test
    fun `envia las instrucciones y la secuencia al generador`() {
        var instrucciones = ""
        var mensaje = ""
        val i = interprete { ins, msg ->
            instrucciones = ins
            mensaje = msg
            "{}"
        }
        ejecutar { i.interpretar(secuencia, contexto = "Hola.") }

        assertEquals(PlantillaPrompt.INSTRUCCIONES, instrucciones)
        assertTrue(mensaje.contains("id=lsc.nombre"))
        assertTrue(mensaje.contains("Frase anterior"))
    }

    @Test
    fun `si el modelo inventa una sena se usa el respaldo`() {
        val i = interprete { _, _ ->
            """{"propuesta": "Me llamo Nik, mucho gusto.", "senas_usadas": ["lsc.yo", "lsc.nombre", "lsc.nik", "lsc.gusto"], "incierta": false}"""
        }
        val r = ejecutar { i.interpretar(secuencia) }

        assertTrue(r is ResultadoGenerativo.Descartada)
        assertTrue((r as ResultadoGenerativo.Descartada).motivos.any { "lsc.gusto" in it })
        assertEquals("lsc.yo lsc.nombre lsc.nik", r.propuesta.texto)
        assertEquals(OrigenInterpretacion.REGLAS, r.propuesta.origen)
    }

    @Test
    fun `si la respuesta no es json se usa el respaldo`() {
        val r = ejecutar { interprete { _, _ -> "Me llamo Nik." }.interpretar(secuencia) }
        assertTrue(r is ResultadoGenerativo.Descartada)
    }

    @Test
    fun `si el modelo lanza una excepcion se usa el respaldo`() {
        val r = ejecutar { interprete { _, _ -> error("sin memoria") }.interpretar(secuencia) }
        assertTrue((r as ResultadoGenerativo.Descartada).motivos.single().contains("sin memoria"))
    }

    @Test
    fun `con una sola sena no se consulta al modelo`() {
        var llamado = false
        val r = ejecutar {
            interprete { _, _ ->
                llamado = true
                "{}"
            }.interpretar(listOf(sena("lsc.hola")))
        }
        assertTrue(r is ResultadoGenerativo.NoAplica)
        assertEquals(false, llamado)
    }

    @Test
    fun `no puede declararse como interprete por reglas`() {
        try {
            InterpreteGenerativo(GeneradorTexto { _, _ -> "" }, OrigenInterpretacion.REGLAS, respaldo)
            fail("Se esperaba IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
