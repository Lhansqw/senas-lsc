package com.lhansqw.senaslsc.core.texto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizarTest {

    @Test
    fun `quita tildes y dieresis y pasa a minusculas`() {
        assertEquals("como estas", normalizarParaBusqueda("¿CÓMO   estás"))
        assertEquals("pinguino", normalizarParaBusqueda("Pingüino"))
    }

    @Test
    fun `la ene se conserva`() {
        assertEquals("señas año", normalizarParaBusqueda("SEÑAS Año"))
    }

    @Test
    fun `todas las palabras de la consulta deben aparecer`() {
        assertTrue(coincideBusqueda("Buenos días, ¿cómo estás?", "como DIAS"))
        assertFalse(coincideBusqueda("Buenos días", "noches"))
        assertTrue(coincideBusqueda("cualquier cosa", "   "))
        assertFalse(coincideBusqueda("señas", "senas"))
    }
}
