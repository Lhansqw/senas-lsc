package com.lhansqw.senaslsc.data.history

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Fecha legible para el historial, en español de Colombia:
 * «Hoy, 14:32», «Ayer, 09:10», «12 oct, 18:05» (este año) o «12 oct 2025, 18:05».
 */
object FormatoFecha {

    /** Abreviaturas de CLDR para es-CO, sin el punto final («oct.» → «oct»). */
    private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic")

    fun relativa(ms: Long, ahoraMs: Long, zona: TimeZone = TimeZone.currentSystemDefault()): String {
        val fecha = Instant.fromEpochMilliseconds(ms).toLocalDateTime(zona)
        val hoy = Instant.fromEpochMilliseconds(ahoraMs).toLocalDateTime(zona).date
        val dia = fecha.date
        val hora = "${dosCifras(fecha.hour)}:${dosCifras(fecha.minute)}"
        val diaMes = "${dia.day} ${MESES[dia.month.number - 1]}"
        return when {
            dia == hoy -> "Hoy, $hora"
            dia == hoy.minus(1, DateTimeUnit.DAY) -> "Ayer, $hora"
            dia.year == hoy.year -> "$diaMes, $hora"
            else -> "$diaMes ${dia.year}, $hora"
        }
    }

    private fun dosCifras(n: Int) = n.toString().padStart(2, '0')
}
