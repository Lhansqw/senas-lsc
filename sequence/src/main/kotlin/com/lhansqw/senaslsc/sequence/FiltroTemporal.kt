package com.lhansqw.senaslsc.sequence

import com.lhansqw.senaslsc.core.model.PrediccionSena
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.core.model.Vocabulario

/**
 * Parámetros del filtro temporal. Los valores por defecto son provisionales: se calibran en la
 * fase 11 con el conjunto de validación (personas no vistas en el entrenamiento).
 */
data class ConfiguracionFiltro(
    /** Probabilidad mínima de la mejor candidata para tenerla en cuenta. */
    val umbralConfianza: Float = 0.70f,
    /** Diferencia mínima con la segunda candidata. Por debajo, la predicción es ambigua. */
    val margenMinimo: Float = 0.15f,
    /** Ventanas seguidas en que debe ganar la misma seña para confirmarla. */
    val ventanasParaConfirmar: Int = 3,
    /**
     * Ventanas sin esa seña necesarias para poder confirmarla otra vez.
     * Evita repetir una seña mientras se mantiene, pero permite repetirla a propósito
     * (por ejemplo, la doble L de LLAVE) bajando la mano un instante.
     */
    val ventanasParaLiberar: Int = 2,
) {
    init {
        require(umbralConfianza in 0f..1f) { "umbralConfianza debe estar entre 0 y 1" }
        require(margenMinimo in 0f..1f) { "margenMinimo debe estar entre 0 y 1" }
        require(ventanasParaConfirmar >= 1) { "ventanasParaConfirmar debe ser al menos 1" }
        require(ventanasParaLiberar >= 1) { "ventanasParaLiberar debe ser al menos 1" }
    }
}

/**
 * Convierte la secuencia de predicciones crudas en señas confirmadas.
 *
 * Una seña se confirma solo si gana en [ConfiguracionFiltro.ventanasParaConfirmar] ventanas
 * seguidas con confianza y margen suficientes. Cualquier ventana desconocida, ambigua o de baja
 * confianza interrumpe la racha. Es la primera barrera contra los falsos positivos: preferimos no
 * mostrar nada a mostrar una seña equivocada.
 *
 * No es seguro usarlo desde varios hilos a la vez: se llama desde un único consumidor.
 */
class FiltroTemporal(
    private val vocabulario: Vocabulario,
    private val configuracion: ConfiguracionFiltro = ConfiguracionFiltro(),
) {
    private var candidatoId: String? = null
    private val racha = mutableListOf<Float>()
    private var inicioRachaMs = 0L

    /** Última seña confirmada que sigue «sostenida»; no se vuelve a emitir hasta liberarla. */
    private var bloqueadaId: String? = null
    private var ventanasSinBloqueada = 0

    /** Procesa una predicción. Devuelve la seña confirmada en esta ventana o `null`. */
    fun procesar(prediccion: PrediccionSena): SenaConfirmada? {
        val mejor = prediccion.mejor
        val valida = mejor != null &&
            !prediccion.desconocida &&
            mejor.probabilidad >= configuracion.umbralConfianza &&
            prediccion.margen >= configuracion.margenMinimo &&
            mejor.id in vocabulario

        if (!valida) {
            reiniciarRacha()
            contarHaciaLiberacion()
            return null
        }
        val id = mejor.id

        if (id == bloqueadaId) {
            // La persona mantiene la seña ya confirmada: no se repite.
            ventanasSinBloqueada = 0
            reiniciarRacha()
            return null
        }
        // Una seña distinta libera la anterior de inmediato.
        bloqueadaId = null
        ventanasSinBloqueada = 0

        if (id != candidatoId) {
            candidatoId = id
            racha.clear()
            inicioRachaMs = prediccion.ventanaInicioMs
        }
        racha += mejor.probabilidad

        if (racha.size < configuracion.ventanasParaConfirmar) return null

        val confirmada = SenaConfirmada(
            entrada = vocabulario.buscar(id)!!,
            confianza = racha.average().toFloat(),
            inicioMs = inicioRachaMs,
            finMs = prediccion.ventanaFinMs,
        )
        bloqueadaId = id
        reiniciarRacha()
        return confirmada
    }

    /** Olvida todo el estado (al pausar o al cerrar la secuencia). */
    fun reiniciar() {
        reiniciarRacha()
        bloqueadaId = null
        ventanasSinBloqueada = 0
    }

    private fun reiniciarRacha() {
        candidatoId = null
        racha.clear()
    }

    private fun contarHaciaLiberacion() {
        if (bloqueadaId == null) return
        ventanasSinBloqueada++
        if (ventanasSinBloqueada >= configuracion.ventanasParaLiberar) {
            bloqueadaId = null
            ventanasSinBloqueada = 0
        }
    }
}
