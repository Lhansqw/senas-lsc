package com.lhansqw.senaslsc.ia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EstadoIaLocalTest {

    @Test
    fun `un telefono no compatible gana sobre todo lo demas`() {
        val e = decidirEstadoIa(compatible = false, motivoNoCompatible = "Poca RAM", instalada = true, activadaPorUsuario = true)
        assertEquals(EstadoIaLocal.NoCompatible("Poca RAM"), e)
    }

    @Test
    fun `sin motivo se usa un texto generico`() {
        val e = decidirEstadoIa(false, null, true, true) as EstadoIaLocal.NoCompatible
        assertTrue(e.motivo.isNotBlank())
    }

    @Test
    fun `sin instalar o desactivada no se carga`() {
        assertEquals(EstadoIaLocal.NoInstalada, decidirEstadoIa(true, null, instalada = false, activadaPorUsuario = true))
        assertEquals(EstadoIaLocal.Desactivada, decidirEstadoIa(true, null, instalada = true, activadaPorUsuario = false))
    }

    @Test
    fun `con todo en orden toca cargar el modelo`() {
        assertEquals(EstadoIaLocal.Cargando, decidirEstadoIa(true, null, instalada = true, activadaPorUsuario = true))
    }

    @Test
    fun `solo lista esta disponible`() {
        assertTrue(EstadoIaLocal.Lista.disponible)
        listOf(EstadoIaLocal.Cargando, EstadoIaLocal.NoInstalada, EstadoIaLocal.Desactivada, EstadoIaLocal.Error("x"))
            .forEach { assertFalse(it.disponible) }
    }
}
