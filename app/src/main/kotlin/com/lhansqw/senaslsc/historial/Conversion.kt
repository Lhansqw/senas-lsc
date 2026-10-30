package com.lhansqw.senaslsc.historial

import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.data.history.OrigenTexto

/** Origen con el que se guarda una propuesta en el historial. */
fun OrigenInterpretacion.aOrigenTexto(): OrigenTexto = when (this) {
    OrigenInterpretacion.REGLAS -> OrigenTexto.REGLAS
    OrigenInterpretacion.LOCAL -> OrigenTexto.IA_LOCAL
    OrigenInterpretacion.REMOTO -> OrigenTexto.IA_REMOTA
    OrigenInterpretacion.USUARIO -> OrigenTexto.USUARIO
}

/**
 * Glosas que acompañan a la frase guardada: las de las señas que la propuesta usa, en el orden en
 * que se hicieron. Así el historial conserva qué se reconoció, además del texto.
 */
fun glosasDe(propuesta: PropuestaFrase, senas: List<SenaConfirmada>): List<String> {
    val usadas = propuesta.senasUsadas.toSet()
    return senas.filter { it.id in usadas }.map { it.entrada.glosa }
}
