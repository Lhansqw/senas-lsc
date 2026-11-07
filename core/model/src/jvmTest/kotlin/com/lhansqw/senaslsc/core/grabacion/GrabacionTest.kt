package com.lhansqw.senaslsc.core.grabacion

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.Punto3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GrabacionTest {

    private val mano = List(21) { i -> Punto3D(0.4f + i * 0.001f, 0.6f, -0.01f) }
    private val cuerpo = List(7) { Punto3D(0.5f, 0.3f + it * 0.05f, 0f) }
    private fun frame(t: Long, manos: Boolean = true) = LandmarkFrame(t, null, if (manos) mano else null, cuerpo, null, 0.75f)

    /** Cámara a 30 FPS desde [desde] hasta que el grabador termina; devuelve el estado final. */
    private fun grabar(g: GrabadorMuestra, desde: Long = 5_000, manos: (Long) -> Boolean = { true }, pasoMs: Long = 33): EstadoGrabacion {
        var t = desde
        while (true) {
            val e = g.recibir(frame(t, manos(t - desde)))
            if (e is EstadoGrabacion.Terminada || e is EstadoGrabacion.Fallida) return e
            t += pasoMs
            if (t > desde + 10_000) fail("No terminó")
        }
    }

    @Test
    fun `cuenta atras, grabacion y muestra con tiempos desde cero`() {
        val g = GrabadorMuestra()
        g.iniciar("yo01", "lsc.s", 3)
        assertEquals(EstadoGrabacion.CuentaAtras(3_000), g.estado)
        assertEquals(EstadoGrabacion.CuentaAtras(3_000), g.recibir(frame(5_000)))
        assertEquals(EstadoGrabacion.CuentaAtras(1_000), g.recibir(frame(7_000)))

        val fin = grabar(g, desde = 7_033) as EstadoGrabacion.Terminada
        val m = fin.muestra
        assertEquals("yo01", m.persona)
        assertEquals("lsc.s", m.sena)
        assertEquals(3, m.repeticion)
        assertTrue("fotogramas: ${m.fotogramas.size}", m.fotogramas.size in 35..38)   // 1,2 s a 30 FPS
        assertTrue(m.fotogramas.first().timestampMs < 33)
        assertTrue(m.fotogramas.last().timestampMs <= 1_200)
        assertTrue(m.fotogramas.zipWithNext().all { (a, b) -> b.timestampMs > a.timestampMs })
    }

    @Test
    fun `una sena sin manos no se guarda pero sin sena si`() {
        val g = GrabadorMuestra()
        g.iniciar("yo01", "lsc.hola", 1)
        val e = grabar(g, manos = { false })
        assertTrue(e is EstadoGrabacion.Fallida && e.motivo.contains("manos"))

        g.iniciar("yo01", FormatoGrabacion.SIN_SENA, 1)
        assertTrue(grabar(g, manos = { false }) is EstadoGrabacion.Terminada)
    }

    @Test
    fun `con la camara muy lenta falla`() {
        val g = GrabadorMuestra()
        g.iniciar("yo01", "lsc.a", 1)
        val e = grabar(g, pasoMs = 250)
        assertTrue(e is EstadoGrabacion.Fallida && e.motivo.contains("lenta"))
    }

    @Test
    fun `sin iniciar o tras cancelar no graba`() {
        val g = GrabadorMuestra()
        assertEquals(EstadoGrabacion.Inactivo, g.recibir(frame(0)))
        g.iniciar("yo01", "lsc.a", 1)
        g.recibir(frame(0))
        g.cancelar()
        assertEquals(EstadoGrabacion.Inactivo, g.recibir(frame(4_000)))
    }

    @Test
    fun `json de ida y vuelta con los campos de los casos compartidos`() {
        val m = MuestraGrabada("ana_2", "lsc.hola", 7, listOf(frame(0), frame(33, manos = false)))
        val texto = FormatoGrabacion.escribir(m)
        listOf("\"formato\":1", "\"fuente\":\"GRABACION_APP\"", "\"t\":33", "\"izquierda\":null", "\"aspecto\":0.75").forEach {
            assertTrue("falta $it", it in texto)
        }
        val leida = FormatoGrabacion.leer(texto)
        assertEquals(m.persona, leida.persona)
        assertEquals(m.repeticion, leida.repeticion)
        assertEquals(2, leida.fotogramas.size)
        assertNull(leida.fotogramas[1].manoDerecha)
        assertEquals(0.42f, leida.fotogramas[0].manoDerecha!![20].x, 1e-5f)
        assertEquals("GRABACION_APP__ana_2__lsc.hola__r7.json", FormatoGrabacion.nombreArchivo(m))
    }

    @Test
    fun `rechaza alias con datos o espacios`() {
        for (alias in listOf("Ana Pérez", "", "a/b", "x".repeat(30))) {
            try {
                FormatoGrabacion.escribir(MuestraGrabada(alias, "lsc.a", 1, listOf(frame(0))))
                fail("Debía rechazar «$alias»")
            } catch (_: IllegalArgumentException) {
            }
        }
    }
}
