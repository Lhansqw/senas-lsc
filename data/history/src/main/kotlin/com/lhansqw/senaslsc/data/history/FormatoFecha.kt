package com.lhansqw.senaslsc.data.history

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Fecha legible para el historial, en español de Colombia:
 * «Hoy, 14:32», «Ayer, 09:10», «12 oct, 18:05» (este año) o «12 oct 2025, 18:05».
 */
object FormatoFecha {

    private val ES: Locale = Locale.forLanguageTag("es-CO")
    private val HORA = DateTimeFormatter.ofPattern("HH:mm", ES)
    private val DIA_MES = DateTimeFormatter.ofPattern("d MMM", ES)
    private val DIA_MES_ANO = DateTimeFormatter.ofPattern("d MMM yyyy", ES)

    fun relativa(ms: Long, ahoraMs: Long, zona: ZoneId = ZoneId.systemDefault()): String {
        val fecha = Instant.ofEpochMilli(ms).atZone(zona)
        val hoy = Instant.ofEpochMilli(ahoraMs).atZone(zona).toLocalDate()
        val dia: LocalDate = fecha.toLocalDate()
        val hora = fecha.format(HORA)
        return when {
            dia == hoy -> "Hoy, $hora"
            dia == hoy.minusDays(1) -> "Ayer, $hora"
            dia.year == hoy.year -> "${limpiar(fecha.format(DIA_MES))}, $hora"
            else -> "${limpiar(fecha.format(DIA_MES_ANO))}, $hora"
        }
    }

    /** Algunas versiones de Java abrevian los meses con punto («oct.»); se quita para que sea uniforme. */
    private fun limpiar(texto: String) = texto.replace(".", "")
}
