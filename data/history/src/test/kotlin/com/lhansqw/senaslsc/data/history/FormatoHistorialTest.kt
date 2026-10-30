package com.lhansqw.senaslsc.data.history

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class FormatoHistorialTest {

    private val entradas = listOf(
        EntradaHistorial("a", 1_000, "Hola, gracias.", listOf("HOLA", "GRACIAS"), OrigenTexto.REGLAS),
        EntradaHistorial("b", 2_000, "Me llamo \"Nik\".\nSaludos", listOf("YO", "NOMBRE"), OrigenTexto.USUARIO),
    )

    private fun ilegible(texto: String) {
        try {
            FormatoHistorial.leer(texto.toByteArray())
            fail("Se esperaba HistorialIlegible")
        } catch (_: HistorialIlegible) {
        }
    }

    @Test
    fun `lo escrito se lee igual`() {
        assertEquals(entradas, FormatoHistorial.leer(FormatoHistorial.escribir(entradas)))
    }

    @Test
    fun `un historial vacio es valido`() {
        assertEquals(emptyList<EntradaHistorial>(), FormatoHistorial.leer(FormatoHistorial.escribir(emptyList())))
    }

    @Test
    fun `rechaza datos danados o de otro formato`() {
        ilegible("no es json")
        ilegible("[]")
        ilegible("""{"formato": 2, "entradas": []}""")
        ilegible("""{"formato": 1, "entradas": [{"id": "a"}]}""")
        ilegible("""{"formato": 1, "entradas": [{"id": "a", "creada_ms": 1, "texto": "x", "glosas": [], "origen": "marciano"}]}""")
        ilegible("""{"formato": 1, "entradas": [{"id": "a", "creada_ms": 1, "texto": "  ", "glosas": [], "origen": "reglas"}]}""")
    }

    @Test
    fun `los origenes se guardan con su clave estable`() {
        OrigenTexto.entries.forEach { assertEquals(it, OrigenTexto.deClave(it.clave)) }
    }
}
