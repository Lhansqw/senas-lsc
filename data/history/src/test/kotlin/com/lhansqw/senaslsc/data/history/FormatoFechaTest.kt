package com.lhansqw.senaslsc.data.history

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class FormatoFechaTest {

    private val bogota = ZoneId.of("America/Bogota")
    private fun ms(ano: Int, mes: Int, dia: Int, h: Int, min: Int) =
        ZonedDateTime.of(ano, mes, dia, h, min, 0, 0, bogota).toInstant().toEpochMilli()

    private val ahora = ms(2026, 10, 29, 20, 0)

    @Test
    fun `hoy y ayer con la hora`() {
        assertEquals("Hoy, 14:32", FormatoFecha.relativa(ms(2026, 10, 29, 14, 32), ahora, bogota))
        assertEquals("Ayer, 09:05", FormatoFecha.relativa(ms(2026, 10, 28, 9, 5), ahora, bogota))
    }

    @Test
    fun `este ano sin el ano y otros anos con el ano`() {
        val esteAno = FormatoFecha.relativa(ms(2026, 10, 12, 18, 5), ahora, bogota)
        val otroAno = FormatoFecha.relativa(ms(2025, 10, 12, 18, 5), ahora, bogota)

        assertEquals("12 oct, 18:05", esteAno)
        assertEquals("12 oct 2025, 18:05", otroAno)
    }

    @Test
    fun `usa la zona horaria indicada para decidir el dia`() {
        // 23:30 en Bogotá ya es el día siguiente en UTC.
        val noche = ms(2026, 10, 29, 23, 30)
        assertEquals("Hoy, 23:30", FormatoFecha.relativa(noche, ms(2026, 10, 29, 23, 45), bogota))
    }
}
