package com.lhansqw.senaslsc.interpreter

import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.SenaConfirmada

/**
 * Respuesta de un modelo de lenguaje ya convertida desde JSON.
 * Los nombres siguen el contrato del doc. 02 (`senas_usadas`, `pregunta_aclaracion`).
 */
data class RespuestaModelo(
    val propuesta: String,
    val senasUsadas: List<String>,
    val incierta: Boolean,
    val motivo: String?,
    val preguntaAclaracion: String?,
)

sealed interface ResultadoValidacion {
    data class Valida(val propuesta: PropuestaFrase) : ResultadoValidacion

    /** La respuesta se descarta y se muestra la del intérprete por reglas. */
    data class Invalida(val errores: List<String>) : ResultadoValidacion
}

/**
 * Comprueba que la respuesta de un modelo de lenguaje cumple el contrato antes de mostrarla.
 *
 * Es la garantía de que el modelo no inventa señas: si usa un identificador que no estaba en la
 * secuencia, o lo repite más veces de las que se reconoció, la respuesta se rechaza.
 */
class ValidadorRespuesta(
    private val umbralSenaDudosa: Float = ReglasInterpretacion.UMBRAL_SENA_DUDOSA,
) {

    fun validar(
        entrada: List<SenaConfirmada>,
        respuesta: RespuestaModelo,
        origen: OrigenInterpretacion,
    ): ResultadoValidacion {
        val errores = mutableListOf<String>()

        if (entrada.size < ReglasInterpretacion.MINIMO_SENAS_MODELO) {
            errores += "Con menos de ${ReglasInterpretacion.MINIMO_SENAS_MODELO} señas no se pide una frase al modelo."
        }

        val texto = respuesta.propuesta.trim()
        if (texto.isEmpty()) errores += "La propuesta está vacía."
        if (texto.length > ReglasInterpretacion.LONGITUD_MAXIMA_PROPUESTA) {
            errores += "La propuesta supera ${ReglasInterpretacion.LONGITUD_MAXIMA_PROPUESTA} caracteres."
        }

        val disponibles = entrada.groupingBy { it.id }.eachCount()
        val usadas = respuesta.senasUsadas.groupingBy { it }.eachCount()

        if (usadas.isEmpty()) errores += "La respuesta no indica qué señas usó."
        val inventadas = usadas.keys - disponibles.keys
        if (inventadas.isNotEmpty()) errores += "Usa señas que no se reconocieron: ${inventadas.sorted()}."
        val repetidas = usadas.filter { (id, veces) -> id in disponibles && veces > disponibles.getValue(id) }.keys
        if (repetidas.isNotEmpty()) errores += "Usa más veces de las reconocidas: ${repetidas.sorted()}."

        val omitidas = disponibles.keys - usadas.keys
        val dudosas = entrada.filter { it.confianza < umbralSenaDudosa }
        if (omitidas.isNotEmpty() && !respuesta.incierta) {
            errores += "Omite señas (${omitidas.sorted()}) sin marcar la propuesta como incierta."
        }
        if (dudosas.isNotEmpty() && !respuesta.incierta) {
            errores += "Hay señas con confianza baja (${dudosas.map { it.id }}) y la propuesta no está marcada como incierta."
        }
        if (respuesta.incierta && respuesta.motivo.isNullOrBlank()) {
            errores += "La propuesta es incierta pero no explica el motivo."
        }

        if (errores.isNotEmpty()) return ResultadoValidacion.Invalida(errores)

        return ResultadoValidacion.Valida(
            PropuestaFrase(
                texto = texto,
                senasUsadas = respuesta.senasUsadas,
                incierta = respuesta.incierta,
                motivo = respuesta.motivo?.trim()?.takeIf { it.isNotEmpty() },
                preguntaAclaracion = respuesta.preguntaAclaracion?.trim()?.takeIf { it.isNotEmpty() },
                origen = origen,
            ),
        )
    }
}
