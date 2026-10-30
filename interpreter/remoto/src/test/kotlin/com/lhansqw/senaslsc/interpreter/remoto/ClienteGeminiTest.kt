package com.lhansqw.senaslsc.interpreter.remoto

import com.lhansqw.senaslsc.core.json.Json
import com.lhansqw.senaslsc.core.json.LectorJson
import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.InetSocketAddress

class ClienteGeminiTest {

    private val respuestaBuena = """
        {"candidates":[{"content":{"role":"model","parts":[
            {"text":"pensando…","thought":true},
            {"text":"{\"propuesta\":\"Hola, me llamo Ana.\","},
            {"text":"\"senas_usadas\":[\"lsc.hola\"],\"incierta\":false}"}]},
          "finishReason":"STOP"}],
         "usageMetadata":{"promptTokenCount":412,"candidatesTokenCount":38,"thoughtsTokenCount":20,"totalTokenCount":470}}
    """.trimIndent()

    private class Peticion(val ruta: String, val clave: String?, val cuerpo: String)

    /** Servidor local que imita a Gemini; devuelve lo que recibió. */
    private fun conServidor(codigo: Int, respuesta: String, retardoMs: Long = 0, bloque: (String) -> Unit): Peticion? {
        var recibida: Peticion? = null
        val servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/") { ex ->
            recibida = Peticion(ex.requestURI.toString(), ex.requestHeaders.getFirst("x-goog-api-key"), ex.requestBody.readBytes().toString(Charsets.UTF_8))
            if (retardoMs > 0) Thread.sleep(retardoMs)
            val datos = respuesta.toByteArray(Charsets.UTF_8)
            ex.sendResponseHeaders(codigo, datos.size.toLong())
            ex.responseBody.use { it.write(datos) }
        }
        servidor.start()
        try {
            bloque("http://127.0.0.1:${servidor.address.port}/v1beta")
        } finally {
            servidor.stop(0)
        }
        return recibida
    }

    private fun error(bloque: () -> Unit): ErrorGemini {
        try {
            bloque()
        } catch (e: ErrorGemini) {
            return e
        }
        fail("Se esperaba un ErrorGemini")
        throw AssertionError()
    }

    @Test
    fun `envia la peticion correcta y lee texto y tokens`() {
        var r: RespuestaGemini? = null
        val p = conServidor(200, respuestaBuena) { base ->
            r = ClienteGemini("clave-secreta", "gemini-3.5-flash-lite", base).generar("INSTRUCCIONES", "señas: HOLA")
        }!!
        assertEquals("/v1beta/models/gemini-3.5-flash-lite:generateContent", p.ruta)
        assertEquals("clave-secreta", p.clave)
        assertFalse("la clave no va en la URL", "clave-secreta" in p.ruta)

        val cuerpo = LectorJson.leer(p.cuerpo) as Json.Objeto
        assertTrue("INSTRUCCIONES" in p.cuerpo && "señas: HOLA" in p.cuerpo)
        assertEquals(Json.Texto("application/json"), (cuerpo["generationConfig"] as Json.Objeto)["responseMimeType"])
        assertTrue(cuerpo["systemInstruction"] is Json.Objeto)

        assertEquals("{\"propuesta\":\"Hola, me llamo Ana.\",\"senas_usadas\":[\"lsc.hola\"],\"incierta\":false}", r!!.texto)
        assertEquals(412, r!!.tokensEntrada)
        assertEquals(58, r!!.tokensSalida)            // salida + razonamiento
    }

    @Test
    fun `errores de la API con mensajes claros`() {
        val claveMala = """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT"}}"""
        conServidor(400, claveMala) { base ->
            assertEquals(ErrorGemini.Tipo.CLAVE_INVALIDA, error { ClienteGemini("x", urlBase = base).generar("a", "b") }.tipo)
        }
        conServidor(429, """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED"}}""") { base ->
            val e = error { ClienteGemini("x", urlBase = base).generar("a", "b") }
            assertEquals(ErrorGemini.Tipo.LIMITE_USO, e.tipo)
            assertTrue(e.message!!.contains("límite"))
        }
        conServidor(503, "no disponible") { base ->
            assertEquals(ErrorGemini.Tipo.SERVIDOR, error { ClienteGemini("x", urlBase = base).generar("a", "b") }.tipo)
        }
        conServidor(403, """{"error":{"code":403,"status":"PERMISSION_DENIED"}}""") { base ->
            assertEquals(ErrorGemini.Tipo.SIN_PERMISO, error { ClienteGemini("x", urlBase = base).generar("a", "b") }.tipo)
        }
    }

    @Test
    fun `respuesta bloqueada o vacia`() {
        val bloqueada = """{"promptFeedback":{"blockReason":"SAFETY"}}"""
        assertEquals(ErrorGemini.Tipo.BLOQUEADA, error { ClienteGemini.leerRespuesta(bloqueada, 0) }.tipo)
        val sinTexto = """{"candidates":[{"content":{"parts":[]},"finishReason":"MAX_TOKENS"}]}"""
        val e = error { ClienteGemini.leerRespuesta(sinTexto, 0) }
        assertEquals(ErrorGemini.Tipo.SIN_RESPUESTA, e.tipo)
        assertTrue(e.message!!.contains("MAX_TOKENS"))
        assertEquals(ErrorGemini.Tipo.SIN_RESPUESTA, error { ClienteGemini.leerRespuesta("<html>", 0) }.tipo)
    }

    @Test
    fun `si tarda demasiado es un error de red`() {
        conServidor(200, respuestaBuena, retardoMs = 1_500) { base ->
            val e = error { ClienteGemini("x", urlBase = base, esperaRespuestaMs = 300).generar("a", "b") }
            assertEquals(ErrorGemini.Tipo.RED, e.tipo)
        }
    }

    @Test
    fun `sin servidor es un error de red`() {
        val e = error { ClienteGemini("x", urlBase = "http://127.0.0.1:1/v1beta", esperaConexionMs = 500).generar("a", "b") }
        assertEquals(ErrorGemini.Tipo.RED, e.tipo)
    }

    @Test
    fun `rechaza clave vacia y nombres de modelo raros`() {
        for ((clave, modelo) in listOf("" to "gemini-3.5-flash-lite", "x" to "../otro", "x" to "modelo?clave=1")) {
            try {
                ClienteGemini(clave, modelo)
                fail("Debía rechazar ($clave, $modelo)")
            } catch (_: IllegalArgumentException) {
            }
        }
    }
}
