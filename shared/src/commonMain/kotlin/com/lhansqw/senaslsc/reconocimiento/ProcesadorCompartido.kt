package com.lhansqw.senaslsc.reconocimiento

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.recognizer.LectorContrato
import com.lhansqw.senaslsc.recognizer.ResultadoContrato
import com.lhansqw.senaslsc.sequence.MotorSenas
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Fachada del reconocimiento para la app de iPhone. Envuelve [CoordinadorReconocimiento] con
 * funciones normales (sin `suspend`) que Swift puede llamar directamente.
 *
 * Igual que en Android, no es seguro para varios hilos: Swift debe llamarla siempre desde la misma
 * cola serie (por ejemplo, la cola de análisis de la cámara).
 *
 * En esta primera versión de iOS las frases las propone solo el intérprete por reglas.
 */
class ProcesadorCompartido private constructor(
    private val coordinador: CoordinadorReconocimiento,
) {
    val estado: EstadoReconocimiento get() = coordinador.estado

    /** Resumen del estado para el reloj (Apple Watch o Wear OS). */
    val resumenReloj: ResumenReloj get() = ResumenReloj.de(coordinador.estado)

    /**
     * Procesa un fotograma ya convertido con [ConstructorFotograma] y devuelve el estado nuevo.
     * En Swift lanza un error si el modelo falla, en vez de cerrar la app.
     */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun agregarFotograma(frame: LandmarkFrame): EstadoReconocimiento {
        sinSuspender { coordinador.alRecibirFotograma(frame) }
        return estado
    }

    /** El usuario pulsa «Interpretar»: cierra la frase aunque no haya pausa. */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun interpretar(): EstadoReconocimiento {
        sinSuspender { coordinador.interpretar() }
        return estado
    }

    fun eliminarUltima(): EstadoReconocimiento {
        coordinador.eliminarUltima()
        return estado
    }

    fun limpiar(): EstadoReconocimiento {
        coordinador.limpiar()
        return estado
    }

    /** Devuelve `false` si no hay frase que corregir o el texto está vacío. */
    fun corregir(texto: String): Boolean = coordinador.corregir(texto)

    companion object {
        /**
         * Crea el procesador a partir del texto de `modelo.json` y de la inferencia de la plataforma.
         * Nunca lanza excepciones: si algo falla, el procesador queda creado y [estado] explica el
         * problema (sin modelo, `modelo.json` inválido o fallo al ejecutar el modelo).
         *
         * @param modeloJson contenido de `modelo.json`, o `null` si la app no lo trae.
         * @param inferencia el modelo `.tflite` ya cargado, o `null` si no se pudo cargar.
         */
        fun crear(modeloJson: String?, inferencia: InferenciaModelo?): ProcesadorCompartido {
            if (modeloJson == null && inferencia == null) return sinReconocedor(EstadoModeloSenas.SinModelo)
            if (modeloJson == null) return sinReconocedor(EstadoModeloSenas.Invalido(listOf("Falta modelo.json junto al modelo.")))
            if (inferencia == null) return sinReconocedor(EstadoModeloSenas.Invalido(listOf("Falta el modelo .tflite junto a modelo.json.")))

            val contrato = when (val r = LectorContrato.leer(modeloJson)) {
                is ResultadoContrato.Invalido -> return sinReconocedor(EstadoModeloSenas.Invalido(r.errores))
                is ResultadoContrato.Valido -> r.contrato
            }
            val reconocedor = ReconocedorExterno(contrato, inferencia)
            try {
                reconocedor.comprobar()
            } catch (e: Exception) {
                return sinReconocedor(EstadoModeloSenas.Fallo("No se pudo ejecutar el modelo: ${e.message ?: e::class.simpleName}"))
            }
            val modelo = EstadoModeloSenas.Listo(
                nombre = contrato.nombre,
                version = contrato.version,
                numeroSenas = contrato.entradas.size,
                metricas = contrato.metricas,
                glosas = contrato.entradas.associate { it.id to it.glosa },
            )
            val motor = MotorSenas(reconocedor.vocabulario, contrato.fotogramas, contrato.duracionVentanaMs)
            return ProcesadorCompartido(CoordinadorReconocimiento(reconocedor, motor, modelo))
        }

        private fun sinReconocedor(modelo: EstadoModeloSenas) =
            ProcesadorCompartido(CoordinadorReconocimiento(null, null, modelo))
    }
}

/**
 * Ejecuta un bloque `suspend` que en realidad nunca se suspende (el coordinador solo es `suspend`
 * por el reconocedor y el intérprete, que aquí responden al momento).
 */
internal fun <T> sinSuspender(bloque: suspend () -> T): T {
    var resultado: Result<T>? = null
    bloque.startCoroutine(Continuation(EmptyCoroutineContext) { resultado = it })
    return checkNotNull(resultado) { "El bloque se suspendió; la fachada solo admite operaciones inmediatas" }.getOrThrow()
}
