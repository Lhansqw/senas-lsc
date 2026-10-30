package com.lhansqw.senaslsc.historial

import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.data.history.OrigenTexto
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversionTest {

    @Test
    fun `cada origen de propuesta tiene su origen en el historial`() {
        assertEquals(OrigenTexto.REGLAS, OrigenInterpretacion.REGLAS.aOrigenTexto())
        assertEquals(OrigenTexto.IA_LOCAL, OrigenInterpretacion.LOCAL.aOrigenTexto())
        assertEquals(OrigenTexto.IA_REMOTA, OrigenInterpretacion.REMOTO.aOrigenTexto())
        assertEquals(OrigenTexto.USUARIO, OrigenInterpretacion.USUARIO.aOrigenTexto())
    }

    @Test
    fun `las glosas siguen el orden de las senas y solo las usadas`() {
        fun sena(id: String, glosa: String) = SenaConfirmada(EntradaVocabulario(id, glosa, glosa.lowercase(), TipoSena.PALABRA), 0.9f, 0, 1)
        val senas = listOf(sena("lsc.yo", "YO"), sena("lsc.nombre", "NOMBRE"), sena("lsc.nik", "NIK"))
        val propuesta = PropuestaFrase("Nik.", listOf("lsc.nik", "lsc.yo"), true, "omitida", null, OrigenInterpretacion.LOCAL)

        assertEquals(listOf("YO", "NIK"), glosasDe(propuesta, senas))
    }
}
