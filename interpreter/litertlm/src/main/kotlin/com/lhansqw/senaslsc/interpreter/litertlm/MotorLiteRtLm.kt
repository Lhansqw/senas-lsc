package com.lhansqw.senaslsc.interpreter.litertlm

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.lhansqw.senaslsc.interpreter.GeneradorTexto
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Modelo de lenguaje en el teléfono con LiteRT-LM.
 *
 * - [crear] carga el modelo; puede tardar hasta unos 10 segundos, nunca en el hilo principal.
 * - Cada petición usa una conversación nueva con las instrucciones del contrato como mensaje de
 *   sistema: las frases anteriores no influyen salvo por el contexto que se pase explícitamente.
 * - Temperatura baja: se busca una frase fiel a las señas, no creatividad.
 * - Si la generación supera [tiempoMaximoMs], se cancela y la app usa la propuesta por reglas.
 *
 * Se usa siempre desde un único hilo de fondo (el de la IA de la app).
 */
class MotorLiteRtLm private constructor(
    private val motor: Engine,
    private val tiempoMaximoMs: Long,
) : GeneradorTexto, AutoCloseable {

    private val vigilante = Executors.newSingleThreadScheduledExecutor { tarea -> Thread(tarea, "senas-ia-vigilante") }

    override suspend fun generar(instrucciones: String, mensaje: String): String {
        val configuracion = ConversationConfig(
            systemInstruction = Contents.of(instrucciones),
            samplerConfig = SamplerConfig(topK = 20, topP = 0.9, temperature = 0.2),
        )
        return motor.createConversation(configuracion).use { conversacion ->
            val alarma = vigilante.schedule({ conversacion.cancelProcess() }, tiempoMaximoMs, TimeUnit.MILLISECONDS)
            try {
                conversacion.sendMessage(mensaje).toString()
            } finally {
                alarma.cancel(false)
            }
        }
    }

    override fun close() {
        vigilante.shutdownNow()
        motor.close()
    }

    companion object {
        const val TIEMPO_MAXIMO_MS = 30_000L

        /**
         * Carga el modelo desde [rutaModelo]. Lanza una excepción si no se puede cargar (archivo
         * dañado, memoria insuficiente…).
         *
         * @param directorioCache carpeta escribible que acelera la segunda carga.
         */
        fun crear(rutaModelo: String, directorioCache: String?, tiempoMaximoMs: Long = TIEMPO_MAXIMO_MS): MotorLiteRtLm {
            val motor = Engine(EngineConfig(modelPath = rutaModelo, backend = Backend.CPU(), cacheDir = directorioCache))
            try {
                motor.initialize()
            } catch (e: Throwable) {
                motor.close()
                throw e
            }
            return MotorLiteRtLm(motor, tiempoMaximoMs)
        }
    }
}
