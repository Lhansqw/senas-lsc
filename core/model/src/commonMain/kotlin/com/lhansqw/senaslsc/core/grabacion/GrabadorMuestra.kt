package com.lhansqw.senaslsc.core.grabacion

import com.lhansqw.senaslsc.core.model.LandmarkFrame

/** En qué punto está una grabación. */
sealed interface EstadoGrabacion {
    data object Inactivo : EstadoGrabacion

    /** Esperando a que la persona se prepare. */
    data class CuentaAtras(val restanteMs: Long) : EstadoGrabacion

    data class Grabando(val transcurridoMs: Long, val fotogramas: Int) : EstadoGrabacion

    data class Terminada(val muestra: MuestraGrabada) : EstadoGrabacion

    /** La grabación no sirve (pocas imágenes o sin manos); no se guarda. */
    data class Fallida(val motivo: String) : EstadoGrabacion
}

/**
 * Graba una muestra a partir de los fotogramas que ya produce MediaPipe (fase 6).
 *
 * Usa solo la marca de tiempo de cada fotograma: la cuenta atrás empieza con el primero que llega
 * tras [iniciar], y la grabación dura [duracionMs] desde que termina. Al acabar comprueba que la
 * muestra sirve para entrenar. Kotlin puro y sin sincronización: usar desde un solo hilo.
 *
 * @param minFotogramas por debajo, la cámara iba demasiado lenta para que la muestra sea útil.
 * @param minProporcionManos fracción de fotogramas con alguna mano exigida a las señas (no a «sin seña»).
 */
class GrabadorMuestra(
    private val cuentaAtrasMs: Long = 3_000L,
    private val duracionMs: Long = 1_200L,
    private val minFotogramas: Int = 10,
    private val minProporcionManos: Float = 0.6f,
) {
    init {
        require(cuentaAtrasMs >= 0 && duracionMs > 0) { "Tiempos no válidos" }
    }

    var estado: EstadoGrabacion = EstadoGrabacion.Inactivo
        private set

    private var persona = ""
    private var sena = ""
    private var repeticion = 0
    private var inicioMs: Long? = null
    private val fotogramas = mutableListOf<LandmarkFrame>()

    fun iniciar(persona: String, sena: String, repeticion: Int) {
        require(FormatoGrabacion.ALIAS_VALIDO.matches(persona)) { "Alias no válido" }
        this.persona = persona
        this.sena = sena
        this.repeticion = repeticion
        inicioMs = null
        fotogramas.clear()
        estado = EstadoGrabacion.CuentaAtras(cuentaAtrasMs)
    }

    fun recibir(frame: LandmarkFrame): EstadoGrabacion {
        val actual = estado
        if (actual !is EstadoGrabacion.CuentaAtras && actual !is EstadoGrabacion.Grabando) return actual
        val inicio = inicioMs ?: frame.timestampMs.also { inicioMs = it }
        val transcurrido = frame.timestampMs - inicio
        if (transcurrido < 0) return cancelarCon("La cámara se reinició durante la grabación.")

        if (transcurrido < cuentaAtrasMs) {
            estado = EstadoGrabacion.CuentaAtras(cuentaAtrasMs - transcurrido)
            return estado
        }
        val enGrabacion = transcurrido - cuentaAtrasMs
        if (enGrabacion <= duracionMs) {
            fotogramas += frame.copy(timestampMs = enGrabacion)
            estado = EstadoGrabacion.Grabando(enGrabacion, fotogramas.size)
            if (enGrabacion < duracionMs) return estado
        }
        estado = terminar()
        return estado
    }

    fun cancelar() {
        fotogramas.clear()
        estado = EstadoGrabacion.Inactivo
    }

    /** Tras guardar o descartar el resultado, para poder grabar otra. */
    fun reiniciar() = cancelar()

    private fun terminar(): EstadoGrabacion {
        if (fotogramas.size < minFotogramas) {
            return EstadoGrabacion.Fallida("Solo se captaron ${fotogramas.size} imágenes: la cámara va demasiado lenta. Prueba con más luz.")
        }
        val conManos = fotogramas.count { it.hayManos }.toFloat() / fotogramas.size
        if (sena != FormatoGrabacion.SIN_SENA && conManos < minProporcionManos) {
            return EstadoGrabacion.Fallida("No se vieron las manos en casi toda la grabación. Colócate más lejos y con las manos dentro de la imagen.")
        }
        return EstadoGrabacion.Terminada(MuestraGrabada(persona, sena, repeticion, fotogramas.toList()))
    }

    private fun cancelarCon(motivo: String): EstadoGrabacion {
        fotogramas.clear()
        estado = EstadoGrabacion.Fallida(motivo)
        return estado
    }
}
