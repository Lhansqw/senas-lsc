package com.lhansqw.senaslsc.permisos

import org.junit.Assert.assertEquals
import org.junit.Test

class EstadoPermisoTest {

    @Test
    fun `concedido gana sobre cualquier otra senal`() {
        assertEquals(EstadoPermiso.CONCEDIDO, resolverEstadoPermiso(concedido = true, yaSolicitado = true, debeMostrarExplicacion = true))
    }

    @Test
    fun `antes de pedirlo se muestra la explicacion inicial`() {
        assertEquals(EstadoPermiso.SIN_SOLICITAR, resolverEstadoPermiso(concedido = false, yaSolicitado = false, debeMostrarExplicacion = false))
    }

    @Test
    fun `tras un primer rechazo se explica de nuevo`() {
        assertEquals(EstadoPermiso.EXPLICAR, resolverEstadoPermiso(concedido = false, yaSolicitado = true, debeMostrarExplicacion = true))
    }

    @Test
    fun `rechazo sin opcion de volver a preguntar lleva a los ajustes`() {
        assertEquals(
            EstadoPermiso.DENEGADO_PERMANENTE,
            resolverEstadoPermiso(concedido = false, yaSolicitado = true, debeMostrarExplicacion = false),
        )
    }
}
