package com.lhansqw.senaslsc.interpreter.remoto

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI

/**
 * Cliente mínimo de `models.generateContent` de la API de Gemini (fase 10).
 *
 * - La clave va en la cabecera `x-goog-api-key`, nunca en la URL (las URL acaban en registros).
 * - Solo se envían las instrucciones y el mensaje de [com.lhansqw.senaslsc.interpreter.PlantillaPrompt]:
 *   glosas, significados y confianzas. Nunca imágenes ni puntos de referencia.
 * - Pide la respuesta en JSON; aun así, la valida después `InterpreteGenerativo`.
 *
 * Es **bloqueante** (HttpURLConnection, disponible en Android y en la JVM): llamarlo desde un hilo
 * de E/S, nunca desde el principal. El cuerpo y la lectura de la respuesta están en [ProtocoloGemini].
 */
class ClienteGemini(
    private val clave: String,
    val modelo: String = MODELO_PREDETERMINADO,
    private val urlBase: String = URL_BASE,
    private val esperaConexionMs: Int = 10_000,
    private val esperaRespuestaMs: Int = 30_000,
    private val reloj: () -> Long = System::nanoTime,
) {
    init {
        require(clave.isNotBlank()) { "Falta la clave de API" }
        require(ProtocoloGemini.modeloValido(modelo)) { "Nombre de modelo no válido: $modelo" }
    }

    fun generar(instrucciones: String, mensaje: String): RespuestaGemini {
        val inicio = reloj()
        val conexion = try {
            (URI("$urlBase/models/$modelo:generateContent").toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = esperaConexionMs
                readTimeout = esperaRespuestaMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("x-goog-api-key", clave)
            }
        } catch (e: IOException) {
            throw ErrorGemini(ErrorGemini.Tipo.RED, "No se pudo conectar con Gemini.", e)
        }
        try {
            conexion.outputStream.use { it.write(ProtocoloGemini.cuerpo(instrucciones, mensaje).toByteArray(Charsets.UTF_8)) }
            val codigo = conexion.responseCode
            val texto = (if (codigo in 200..299) conexion.inputStream else conexion.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (codigo !in 200..299) throw ProtocoloGemini.errorHttp(codigo, texto)
            return ProtocoloGemini.leerRespuesta(texto, (reloj() - inicio) / 1_000_000)
        } catch (e: SocketTimeoutException) {
            throw ErrorGemini(ErrorGemini.Tipo.RED, "Gemini tardó demasiado en responder.", e)
        } catch (e: IOException) {
            throw ErrorGemini(ErrorGemini.Tipo.RED, "Sin conexión con Gemini: ${e.message ?: e.javaClass.simpleName}", e)
        } finally {
            conexion.disconnect()
        }
    }

    companion object {
        const val URL_BASE = ProtocoloGemini.URL_BASE
        const val MODELO_PREDETERMINADO = ProtocoloGemini.MODELO_PREDETERMINADO
    }
}
