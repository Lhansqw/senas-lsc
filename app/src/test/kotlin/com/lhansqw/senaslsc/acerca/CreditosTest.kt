package com.lhansqw.senaslsc.acerca

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditosTest {

    private val todos = Creditos.componentes + Creditos.datos

    @Test
    fun `todos los componentes tienen uso, licencia y enlace https`() {
        todos.forEach { c ->
            assertTrue(c.nombre, c.uso.isNotBlank() && c.licencia.isNotBlank())
            assertTrue(c.enlace, c.enlace.startsWith("https://"))
        }
        assertEquals(todos.size, todos.map { it.nombre }.toSet().size)
    }

    @Test
    fun `la cita de LSC70 cumple CC BY 4_0`() {
        // Atribución: autores, título, fuente (DOI) y licencia.
        listOf("LSC70", "Universidad del Cauca", "10.17632/9ssyn8tff5.2", "CC BY 4.0").forEach {
            assertTrue("falta «$it» en la cita", it in Creditos.CITA_LSC70)
        }
        assertEquals("CC BY 4.0", Creditos.datos.licencia)
    }

    @Test
    fun `aparecen las bibliotecas que llevan modelos dentro de la app`() {
        val nombres = Creditos.componentes.joinToString { it.nombre }
        listOf("MediaPipe", "LiteRT", "LiteRT-LM", "Gemma").forEach { assertTrue("falta $it", it in nombres) }
    }
}
