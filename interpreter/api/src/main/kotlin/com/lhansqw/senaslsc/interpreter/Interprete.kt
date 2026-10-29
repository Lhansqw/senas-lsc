package com.lhansqw.senaslsc.interpreter

import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.SenaConfirmada

/**
 * Convierte una secuencia de señas confirmadas en una frase propuesta.
 *
 * Implementaciones: reglas (siempre disponible), modelo local (LiteRT-LM, fase 8) y proveedor
 * remoto (fase 10, desactivado por defecto). Todas cumplen las mismas reglas:
 * - no añaden señas que no estén en la secuencia;
 * - marcan la incertidumbre y explican su motivo;
 * - con una sola seña muestran su significado, no una oración.
 */
interface Interprete {

    val origen: OrigenInterpretacion

    /**
     * @param secuencia señas confirmadas, en el orden en que se hicieron. No puede estar vacía.
     * @param contexto frase anterior ya revisada por el usuario, si existe.
     */
    suspend fun proponer(secuencia: List<SenaConfirmada>, contexto: String? = null): PropuestaFrase
}

/** Valores compartidos por todos los intérpretes. */
object ReglasInterpretacion {

    /**
     * Confianza por debajo de la cual una seña se considera dudosa y la propuesta se marca como
     * incierta. Provisional: se calibra en la fase 11.
     */
    const val UMBRAL_SENA_DUDOSA = 0.75f

    /** Longitud máxima de una propuesta. Una frase mucho más larga que la secuencia es sospechosa. */
    const val LONGITUD_MAXIMA_PROPUESTA = 300

    /** Número mínimo de señas para pedir una frase a un modelo de lenguaje. */
    const val MINIMO_SENAS_MODELO = 2
}
