package com.lhansqw.senaslsc.interpreter

import com.lhansqw.senaslsc.interpreter.EleccionInterprete.INTERNET
import com.lhansqw.senaslsc.interpreter.EleccionInterprete.REGLAS
import com.lhansqw.senaslsc.interpreter.EleccionInterprete.TELEFONO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SelectorInterpreteTest {

    private fun elegir(p: PreferenciaInterprete, telefono: Boolean, permitido: Boolean, conexion: Boolean) =
        SelectorInterprete.elegir(p, SituacionInterprete(telefono, permitido, conexion))

    @Test
    fun `automatico prefiere el telefono, luego internet, luego reglas`() {
        assertEquals(TELEFONO, elegir(PreferenciaInterprete.AUTOMATICO, telefono = true, permitido = true, conexion = true))
        assertEquals(INTERNET, elegir(PreferenciaInterprete.AUTOMATICO, telefono = false, permitido = true, conexion = true))
        assertEquals(REGLAS, elegir(PreferenciaInterprete.AUTOMATICO, telefono = false, permitido = true, conexion = false))
    }

    @Test
    fun `preferir internet cae al telefono sin conexion`() {
        assertEquals(INTERNET, elegir(PreferenciaInterprete.PREFERIR_INTERNET, telefono = true, permitido = true, conexion = true))
        assertEquals(TELEFONO, elegir(PreferenciaInterprete.PREFERIR_INTERNET, telefono = true, permitido = true, conexion = false))
        assertEquals(REGLAS, elegir(PreferenciaInterprete.PREFERIR_INTERNET, telefono = false, permitido = true, conexion = false))
    }

    @Test
    fun `sin consentimiento nunca se usa internet`() {
        for (p in PreferenciaInterprete.entries) for (t in listOf(true, false)) {
            assertNotEquals("$p, teléfono $t", INTERNET, elegir(p, telefono = t, permitido = false, conexion = true))
        }
    }

    @Test
    fun `solo telefono nunca usa internet`() {
        assertEquals(REGLAS, elegir(PreferenciaInterprete.SOLO_TELEFONO, telefono = false, permitido = true, conexion = true))
        assertEquals(TELEFONO, elegir(PreferenciaInterprete.SOLO_TELEFONO, telefono = true, permitido = true, conexion = true))
    }
}
