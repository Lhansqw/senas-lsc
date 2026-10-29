package com.lhansqw.senaslsc.sequence

import com.lhansqw.senaslsc.core.model.Candidata
import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.PrediccionSena
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorSenasTest {

    private fun motor() = MotorSenas(
        vocabulario = vocabularioPrueba,
        fotogramasModelo = 16,
        duracionVentanaMs = 1_000,
        pasoMs = 250,
        pausaCierreMs = 1_200,
    )

    /** «Modelo» de prueba: devuelve siempre la seña indicada para cada ventana. */
    private fun predecir(ventana: List<LandmarkFrame>, id: String?) =
        PrediccionSena(
            ventana.first().timestampMs,
            ventana.last().timestampMs,
            listOf(Candidata(id ?: "lsc.hola", if (id == null) 0.2f else 0.9f)),
            desconocida = id == null,
        )

    /** Simula [ms] milisegundos de cámara a 30 FPS haciendo la seña [id] (o nada si es null). */
    private fun simular(m: MotorSenas, desdeMs: Long, ms: Long, id: String?, conManos: Boolean = true): Long {
        var t = desdeMs
        while (t < desdeMs + ms) {
            val paso = m.alRecibirFotograma(frame(t, conManos))
            paso.ventana?.let { m.alRecibirPrediccion(predecir(it, id)) }
            t += 33
        }
        return t
    }

    @Test
    fun `entrega ventanas remuestreadas al tamano del modelo`() {
        val m = motor()
        val ventanas = (0..60).mapNotNull { m.alRecibirFotograma(frame(it * 33L)).ventana }

        assertTrue(ventanas.isNotEmpty())
        ventanas.forEach { assertEquals(16, it.size) }
    }

    @Test
    fun `una sena mantenida se confirma una vez`() {
        val m = motor()
        simular(m, 0, 3_000, "lsc.hola")

        assertEquals(listOf("lsc.hola"), m.senas.map { it.id })
    }

    @Test
    fun `dos senas seguidas forman una secuencia que se cierra al bajar las manos`() {
        val m = motor()
        var t = simular(m, 0, 2_500, "lsc.hola")
        t = simular(m, t, 2_500, "lsc.gracias")
        assertFalse(m.cerrada)

        var cerrada = false
        repeat(60) {
            cerrada = cerrada || m.alRecibirFotograma(frame(t, conManos = false)).secuenciaCerrada
            t += 33
        }
        assertTrue(cerrada)
        assertTrue(m.cerrada)
        assertEquals(listOf("lsc.hola", "lsc.gracias"), m.senas.map { it.id })
    }

    @Test
    fun `lo que el modelo no reconoce no entra en la secuencia`() {
        val m = motor()
        simular(m, 0, 3_000, id = null)
        assertTrue(m.senas.isEmpty())
    }

    @Test
    fun `cerrar eliminar y limpiar`() {
        val m = motor()
        simular(m, 0, 2_500, "lsc.hola")
        assertTrue(m.cerrar())
        assertNotNull(m.eliminarUltima())
        assertTrue(m.senas.isEmpty())

        simular(m, 3_000, 2_500, "lsc.gracias")
        m.limpiar()
        assertTrue(m.senas.isEmpty())
        // Tras limpiar, la ventana empieza de cero: el siguiente fotograma no completa ventana.
        assertNull(m.alRecibirFotograma(frame(10_000)).ventana)
    }
}
