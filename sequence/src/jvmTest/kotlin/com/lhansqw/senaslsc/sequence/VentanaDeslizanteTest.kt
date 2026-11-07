package com.lhansqw.senaslsc.sequence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class VentanaDeslizanteTest {

    @Test
    fun `no emite hasta llenar la ventana y luego emite cada paso`() {
        val ventana = VentanaDeslizante(tamano = 4, paso = 2)
        val emitidas = (0 until 10).map { ventana.agregar(frame(it * 33L)) }

        // Fotogramas 0-2: aún no hay 4. El 3 llena la ventana. Después, una cada 2 fotogramas.
        val indicesEmitidos = emitidas.withIndex().filter { it.value != null }.map { it.index }
        assertEquals(listOf(3, 5, 7, 9), indicesEmitidos)
        assertEquals(listOf(6L, 7L, 8L, 9L).map { it * 33 }, emitidas[9]!!.map { it.timestampMs })
    }

    @Test
    fun `descarta ventanas sin manos suficientes`() {
        val ventana = VentanaDeslizante(tamano = 4, paso = 1, minProporcionManos = 0.5f)
        ventana.agregar(frame(0, conManos = false))
        ventana.agregar(frame(33, conManos = false))
        ventana.agregar(frame(66, conManos = false))

        assertNull(ventana.agregar(frame(99, conManos = true)))      // 1 de 4 con manos
        assertNotNull(ventana.agregar(frame(132, conManos = true)))  // 2 de 4 con manos
    }

    @Test
    fun `reiniciar obliga a llenar de nuevo la ventana`() {
        val ventana = VentanaDeslizante(tamano = 3, paso = 1)
        repeat(3) { ventana.agregar(frame(it * 33L)) }
        ventana.reiniciar()

        assertNull(ventana.agregar(frame(200)))
        assertNull(ventana.agregar(frame(233)))
        assertNotNull(ventana.agregar(frame(266)))
    }

    @Test
    fun `rechaza fotogramas fuera de orden y parametros invalidos`() {
        val ventana = VentanaDeslizante(tamano = 3, paso = 1)
        ventana.agregar(frame(100))
        try {
            ventana.agregar(frame(50))
            fail("Se esperaba IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
        try {
            VentanaDeslizante(tamano = 3, paso = 4)
            fail("Se esperaba IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
