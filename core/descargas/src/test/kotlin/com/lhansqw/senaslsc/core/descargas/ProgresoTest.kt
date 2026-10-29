package com.lhansqw.senaslsc.core.descargas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgresoTest {

    private val MB = 1024L * 1024

    @Test
    fun `fraccion y tiempo restante`() {
        val p = Progreso(bytes = 250 * MB, total = 1_000 * MB, velocidad = (5 * MB).toDouble())
        assertEquals(0.25f, p.fraccion!!, 1e-6f)
        assertEquals(150L, p.segundosRestantes)
    }

    @Test
    fun `sin total no hay fraccion ni tiempo restante`() {
        val p = Progreso(100, null, 10.0)
        assertNull(p.fraccion)
        assertNull(p.segundosRestantes)
        assertNull(Progreso(100, 1_000, 0.0).segundosRestantes)
    }

    @Test
    fun `la velocidad se mide sobre la ventana reciente`() {
        val m = MedidorProgreso(total = 100 * MB, ventanaMs = 5_000)
        m.registrar(0, 0)
        m.registrar(1_000, 1 * MB)
        assertEquals(MB.toDouble(), m.instantanea().velocidad, 1.0)

        // Tras una ventana entera más rápida, la velocidad antigua ya no cuenta.
        var bytes = MB
        for (s in 2..12) {
            bytes += 4 * MB
            m.registrar(s * 1_000L, bytes)
        }
        assertEquals((4 * MB).toDouble(), m.instantanea().velocidad, 1.0)
    }

    @Test
    fun `al reanudar parte de los bytes ya descargados`() {
        val m = MedidorProgreso(total = 10 * MB, inicialBytes = 6 * MB)
        assertEquals(6 * MB, m.instantanea().bytes)
        assertEquals(0.6f, m.instantanea().fraccion!!, 1e-6f)
    }

    @Test
    fun `espacio requerido con margen`() {
        val gb = 1024 * MB
        // 2,5 GB: el 10 % (256 MB) supera el mínimo de 200 MB.
        assertEquals((2_560 + 256) * MB, EspacioNecesario.requerido(2_560 * MB))
        // Si ya hay 2 GB descargados solo falta el resto más el margen.
        assertEquals((512 + 256) * MB, EspacioNecesario.requerido(2_560 * MB, 2 * gb))
        // Archivo pequeño: se aplica el margen mínimo.
        assertEquals(10 * MB + EspacioNecesario.MARGEN_MINIMO, EspacioNecesario.requerido(10 * MB))

        assertTrue(EspacioNecesario.suficiente(libres = 3 * gb, total = 2_560 * MB))
        assertFalse(EspacioNecesario.suficiente(libres = 2 * gb, total = 2_560 * MB))
    }
}
