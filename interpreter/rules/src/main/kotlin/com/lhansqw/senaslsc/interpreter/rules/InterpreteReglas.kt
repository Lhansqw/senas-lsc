package com.lhansqw.senaslsc.interpreter.rules

import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.interpreter.Interprete
import com.lhansqw.senaslsc.interpreter.ReglasInterpretacion
import com.lhansqw.senaslsc.interpreter.formatearConfianza

/**
 * Intérprete determinista. Siempre está disponible, no necesita Internet ni memoria extra y
 * nunca añade contenido: solo une los significados reconocidos.
 *
 * - Una seña: su significado, sin convertirlo en oración.
 * - Letras seguidas: una palabra deletreada (`N`, `I`, `K` → `NIK`).
 * - Varias señas: significados en orden, mayúscula inicial y punto final; entre signos de
 *   interrogación si alguna seña es interrogativa.
 * - Incierta si alguna seña tiene confianza baja, indicando cuáles.
 *
 * Es también la respuesta de reserva cuando un modelo de lenguaje falla o no cumple el contrato.
 */
class InterpreteReglas(
    private val umbralSenaDudosa: Float = ReglasInterpretacion.UMBRAL_SENA_DUDOSA,
) : Interprete {

    override val origen = OrigenInterpretacion.REGLAS

    override suspend fun proponer(secuencia: List<SenaConfirmada>, contexto: String?): PropuestaFrase =
        componer(secuencia)

    /** Versión síncrona: la lógica no espera nada, así que se puede llamar directamente. */
    fun componer(secuencia: List<SenaConfirmada>): PropuestaFrase {
        require(secuencia.isNotEmpty()) { "La secuencia está vacía" }

        val palabras = agruparDeletreo(secuencia)
        val unida = palabras.joinToString(" ")
        val texto = when {
            palabras.size == 1 -> mayusculaInicial(unida)
            secuencia.any { it.entrada.interrogativa } -> "¿${mayusculaInicial(unida)}?"
            else -> "${mayusculaInicial(unida)}."
        }

        val dudosas = secuencia.filter { it.confianza < umbralSenaDudosa }
        val motivo = dudosas.takeIf { it.isNotEmpty() }?.joinToString(
            prefix = "Confianza baja en: ",
            separator = ", ",
        ) { "${it.entrada.glosa} (${formatearConfianza(it.confianza)})" }

        return PropuestaFrase(
            texto = texto,
            senasUsadas = secuencia.map { it.id },
            incierta = dudosas.isNotEmpty(),
            motivo = motivo,
            preguntaAclaracion = null,
            origen = origen,
        )
    }

    /** Las letras consecutivas se unen en una sola palabra; el resto aporta su significado. */
    private fun agruparDeletreo(secuencia: List<SenaConfirmada>): List<String> {
        val palabras = mutableListOf<String>()
        val deletreo = StringBuilder()
        for (sena in secuencia) {
            if (sena.entrada.tipo == TipoSena.LETRA) {
                deletreo.append(sena.entrada.glosa.uppercase())
            } else {
                if (deletreo.isNotEmpty()) {
                    palabras += deletreo.toString()
                    deletreo.clear()
                }
                palabras += sena.entrada.significado
            }
        }
        if (deletreo.isNotEmpty()) palabras += deletreo.toString()
        return palabras
    }

    private fun mayusculaInicial(texto: String): String =
        texto.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
