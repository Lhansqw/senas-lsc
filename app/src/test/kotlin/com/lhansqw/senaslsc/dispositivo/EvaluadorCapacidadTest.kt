package com.lhansqw.senaslsc.dispositivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluadorCapacidadTest {

    private fun perfil(
        api: Int = 34,
        ramMb: Long = 8_000,
        bajaMemoria: Boolean = false,
        nucleos: Int = 8,
    ) = PerfilDispositivo(api, ramMb, bajaMemoria, nucleos)

    @Test
    fun `telefono de gama alta permite reconocedor e interprete local`() {
        val capacidad = EvaluadorCapacidad.evaluar(perfil())

        assertTrue(capacidad.reconocedorLocal)
        assertTrue(capacidad.interpreteLocal)
        assertTrue(capacidad.motivos.isEmpty())
    }

    @Test
    fun `telefono de 6 GB que reporta menos RAM sigue siendo apto`() {
        val capacidad = EvaluadorCapacidad.evaluar(perfil(ramMb = 5_600))

        assertTrue(capacidad.interpreteLocal)
    }

    @Test
    fun `telefono con poca RAM usa el interprete por reglas`() {
        val capacidad = EvaluadorCapacidad.evaluar(perfil(ramMb = 3_800))

        assertTrue(capacidad.reconocedorLocal)
        assertFalse(capacidad.interpreteLocal)
        assertEquals(1, capacidad.motivos.size)
        assertTrue(capacidad.motivos[0].contains("3,7 GB"))
    }

    @Test
    fun `dispositivo marcado como de baja memoria no carga el modelo de lenguaje`() {
        val capacidad = EvaluadorCapacidad.evaluar(perfil(bajaMemoria = true))

        assertTrue(capacidad.reconocedorLocal)
        assertFalse(capacidad.interpreteLocal)
    }

    @Test
    fun `API inferior a 26 no permite ningun componente`() {
        val capacidad = EvaluadorCapacidad.evaluar(perfil(api = 25))

        assertFalse(capacidad.reconocedorLocal)
        assertFalse(capacidad.interpreteLocal)
        assertEquals(1, capacidad.motivos.size)
    }

    @Test
    fun `pocos nucleos impiden el reconocimiento en tiempo real`() {
        val capacidad = EvaluadorCapacidad.evaluar(perfil(nucleos = 2))

        assertFalse(capacidad.reconocedorLocal)
        assertFalse(capacidad.interpreteLocal)
    }

    @Test
    fun `formatea gigabytes con coma decimal`() {
        assertEquals("5,6 GB", EvaluadorCapacidad.formatearGb(5_734))
        assertEquals("8,0 GB", EvaluadorCapacidad.formatearGb(8_192))
        assertEquals("3,7 GB", EvaluadorCapacidad.formatearGb(3_800))
    }
}
