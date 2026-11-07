package com.lhansqw.senaslsc.sequence

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class RemuestreoTest {

    /** Fotogramas marcados con su índice en la relación de aspecto, para saber cuál se eligió. */
    private fun marcados(vararg tiempos: Long) =
        tiempos.mapIndexed { i, t -> LandmarkFrame(t, null, null, null, null, relacionAspecto = (i + 1).toFloat()) }

    private fun indices(r: List<LandmarkFrame>) = r.map { it.relacionAspecto.toInt() - 1 }

    @Test
    fun `genera instantes equiespaciados que terminan en el ultimo fotograma`() {
        val r = Remuestreo.aTiempoUniforme(marcados(1_000, 1_500, 2_000), cantidad = 5, duracionMs = 1_000)

        assertEquals(listOf(1_000L, 1_250L, 1_500L, 1_750L, 2_000L), r.map { it.timestampMs })
    }

    @Test
    fun `toma el fotograma mas cercano y en empate el anterior`() {
        val r = Remuestreo.aTiempoUniforme(marcados(1_000, 1_500, 2_000), cantidad = 5, duracionMs = 1_000)

        // 1 250 está a 250 ms de 1 000 y de 1 500: empate → el anterior (índice 0).
        assertEquals(listOf(0, 0, 1, 1, 2), indices(r))
    }

    @Test
    fun `a pocos fps repite fotogramas y a muchos fps los salta`() {
        val lento = Remuestreo.aTiempoUniforme(marcados(0, 500, 1_000), cantidad = 11, duracionMs = 1_000)
        assertEquals(11, lento.size)

        val rapido = Remuestreo.aTiempoUniforme(marcados(*LongArray(31) { it * 33L + 10 }), cantidad = 3, duracionMs = 1_000)
        assertEquals(listOf(0, 15, 30), indices(rapido))
    }

    @Test
    fun `si faltan fotogramas al principio usa el primero disponible`() {
        val r = Remuestreo.aTiempoUniforme(marcados(1_800, 2_000), cantidad = 3, duracionMs = 1_000)
        assertEquals(listOf(0, 0, 1), indices(r))
    }

    @Test
    fun `valida los parametros`() {
        listOf(
            { Remuestreo.aTiempoUniforme(emptyList(), 3, 1_000) },
            { Remuestreo.aTiempoUniforme(marcados(0), 1, 1_000) },
            { Remuestreo.aTiempoUniforme(marcados(0), 3, 0) },
        ).forEach { bloque ->
            try {
                bloque()
                fail("Se esperaba IllegalArgumentException")
            } catch (_: IllegalArgumentException) {
            }
        }
    }
}
