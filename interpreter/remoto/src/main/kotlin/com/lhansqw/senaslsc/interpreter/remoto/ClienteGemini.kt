package com.lhansqw.senaslsc.interpreter.remoto

import com.lhansqw.senaslsc.core.json.EscritorJson
import com.lhansqw.senaslsc.core.json.Json
import com.lhansqw.senaslsc.core.json.JsonInvalido
import com.lhansqw.senaslsc.core.json.LectorJson
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI

/** Texto generado por Gemini y lo que costó obtenerlo. */
data class RespuestaGemini(
    val texto: String,
    val tokensEntrada: Int,
    /** Incluye los tokens de razonamiento, que se cobran como salida. */
    val tokensSalida: Int,
    val latenciaMs: Long,
)

/** Por qué falló una llamada, con un mensaje que se puede mostrar al usuario. */
class ErrorGemini(val tipo: Tipo, mensaje: String, causa: Throwable? = null) : IOException(mensaje, causa) {
    enum class Tipo { CLAVE_INVALIDA, SIN_PERMISO, LIMITE_USO, BLOQUEADA, SIN_RESPUESTA, SERVIDOR, RED }
}

/**
 * Cliente mínimo de `models.generateContent` de la API de Gemini (fase 10).
 *
 * - La clave va en la cabecera `x-goog-api-key`, nunca en la URL (las URL acaban en registros).
 * - Solo se envían las instrucciones y el mensaje de [com.lhansqw.senaslsc.interpreter.PlantillaPrompt]:
 *   glosas, significados y confianzas. Nunca imágenes ni puntos de referencia.
 * - Pide la respuesta en JSON; aun así, la valida después `InterpreteGenerativo`.
 *
 * Es **bloqueante** (HttpURLConnection, disponible en Android y en la JVM): llamarlo desde un hilo
 * de E/S, nunca desde el principal.
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
        require(MODELO_VALIDO.matches(modelo)) { "Nombre de modelo no válido: $modelo" }
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
            conexion.outputStream.use { it.write(cuerpo(instrucciones, mensaje).toByteArray(Charsets.UTF_8)) }
            val codigo = conexion.responseCode
            val texto = (if (codigo in 200..299) conexion.inputStream else conexion.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (codigo !in 200..299) throw errorHttp(codigo, texto)
            return leerRespuesta(texto, (reloj() - inicio) / 1_000_000)
        } catch (e: ErrorGemini) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw ErrorGemini(ErrorGemini.Tipo.RED, "Gemini tardó demasiado en responder.", e)
        } catch (e: IOException) {
            throw ErrorGemini(ErrorGemini.Tipo.RED, "Sin conexión con Gemini: ${e.message ?: e.javaClass.simpleName}", e)
        } finally {
            conexion.disconnect()
        }
    }

    companion object {
        const val URL_BASE = "https://generativelanguage.googleapis.com/v1beta"

        /** Rápido y con nivel gratuito (consultado en octubre de 2026). Se puede cambiar en la app. */
        const val MODELO_PREDETERMINADO = "gemini-3.5-flash-lite"

        private val MODELO_VALIDO = Regex("^[a-z0-9][a-z0-9.\\-]{2,63}$")

        /** Cuerpo de la petición. Temperatura baja: se quiere la frase más directa, no creatividad. */
        fun cuerpo(instrucciones: String, mensaje: String): String = EscritorJson.escribir(
            Json.Objeto(
                linkedMapOf(
                    "systemInstruction" to Json.Objeto(mapOf("parts" to Json.Lista(listOf(parte(instrucciones))))),
                    "contents" to Json.Lista(
                        listOf(Json.Objeto(linkedMapOf("role" to Json.Texto("user"), "parts" to Json.Lista(listOf(parte(mensaje)))))),
                    ),
                    "generationConfig" to Json.Objeto(
                        linkedMapOf(
                            "temperature" to Json.Numero(0.2),
                            "maxOutputTokens" to Json.Numero(1024.0),
                            "responseMimeType" to Json.Texto("application/json"),
                        ),
                    ),
                ),
            ),
        )

        private fun parte(texto: String) = Json.Objeto(mapOf("text" to Json.Texto(texto)))

        /** Extrae el texto (sin las partes de razonamiento) y el uso de tokens. */
        fun leerRespuesta(texto: String, latenciaMs: Long): RespuestaGemini {
            val raiz = try {
                LectorJson.leer(texto) as? Json.Objeto
            } catch (e: JsonInvalido) {
                null
            } ?: throw ErrorGemini(ErrorGemini.Tipo.SIN_RESPUESTA, "Gemini devolvió una respuesta que no es JSON.")

            val bloqueo = ((raiz["promptFeedback"] as? Json.Objeto)?.get("blockReason") as? Json.Texto)?.valor
            if (bloqueo != null) throw ErrorGemini(ErrorGemini.Tipo.BLOQUEADA, "Gemini bloqueó la petición ($bloqueo).")

            val candidato = (raiz["candidates"] as? Json.Lista)?.elementos?.firstOrNull() as? Json.Objeto
                ?: throw ErrorGemini(ErrorGemini.Tipo.SIN_RESPUESTA, "Gemini no devolvió ninguna respuesta.")
            val partes = ((candidato["content"] as? Json.Objeto)?.get("parts") as? Json.Lista)?.elementos.orEmpty()
            val salida = partes.filterIsInstance<Json.Objeto>()
                .filter { (it["thought"] as? Json.Booleano)?.valor != true }
                .mapNotNull { (it["text"] as? Json.Texto)?.valor }
                .joinToString("")
            if (salida.isBlank()) {
                val motivo = (candidato["finishReason"] as? Json.Texto)?.valor ?: "desconocido"
                val tipo = if (motivo == "SAFETY" || motivo == "PROHIBITED_CONTENT") ErrorGemini.Tipo.BLOQUEADA else ErrorGemini.Tipo.SIN_RESPUESTA
                throw ErrorGemini(tipo, "Gemini terminó sin texto (motivo: $motivo).")
            }

            val uso = raiz["usageMetadata"] as? Json.Objeto
            fun tokens(clave: String) = (uso?.get(clave) as? Json.Numero)?.valor?.toInt() ?: 0
            return RespuestaGemini(
                texto = salida,
                tokensEntrada = tokens("promptTokenCount"),
                tokensSalida = tokens("candidatesTokenCount") + tokens("thoughtsTokenCount"),
                latenciaMs = latenciaMs,
            )
        }

        /** Traduce el error de la API a un mensaje claro. El cuerpo nunca contiene la clave. */
        fun errorHttp(codigo: Int, cuerpo: String): ErrorGemini {
            val error = runCatching { (LectorJson.leer(cuerpo) as? Json.Objeto)?.get("error") as? Json.Objeto }.getOrNull()
            val estado = (error?.get("status") as? Json.Texto)?.valor
            val detalle = (error?.get("message") as? Json.Texto)?.valor?.take(200)
            val claveMala = codigo == 400 && (cuerpo.contains("API_KEY_INVALID") || detalle?.contains("API key", ignoreCase = true) == true)
            return when {
                claveMala || codigo == 401 -> ErrorGemini(ErrorGemini.Tipo.CLAVE_INVALIDA, "La clave de API de Gemini no es válida.")
                codigo == 403 -> ErrorGemini(ErrorGemini.Tipo.SIN_PERMISO, "La clave no tiene permiso para usar Gemini (o la API no está activada en su proyecto).")
                codigo == 404 -> ErrorGemini(ErrorGemini.Tipo.SIN_PERMISO, "El modelo pedido no existe o no está disponible para esta clave.")
                codigo == 429 -> ErrorGemini(ErrorGemini.Tipo.LIMITE_USO, "Se alcanzó el límite de uso de Gemini. Prueba más tarde.")
                codigo >= 500 -> ErrorGemini(ErrorGemini.Tipo.SERVIDOR, "Gemini no está disponible ahora (error $codigo).")
                else -> ErrorGemini(ErrorGemini.Tipo.SIN_RESPUESTA, "Gemini rechazó la petición (error $codigo${estado?.let { ", $it" } ?: ""}${detalle?.let { ": $it" } ?: ""}).")
            }
        }
    }
}
