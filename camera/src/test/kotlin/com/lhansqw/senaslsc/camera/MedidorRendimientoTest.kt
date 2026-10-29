package com.lhansqw.senaslsc.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class MedidorRendimientoTest {

    private val ms = 1_000_000L

    @Test
    fun `sin fotogramas todo vale cero`() {
        assertEquals(Rendimiento.VACIO, MedidorRendimiento().instantanea())
    }

    @Test
    fun `calcula 30 fps con fotogramas cada 33 ms`() {
        val medidor = MedidorRendimiento()
        repeat(31) { i -> medidor.registrar(timestampNs = i * 33_333_333L, duracionNs = 5 * ms) }

        val r = medidor.instantanea()
        assertEquals(30f, r.fps, 0.1f)
        assertEquals(5f, r.latenciaMediaMs, 0.001f)
        assertEquals(31L, r.analizados)
    }

    @Test
    fun `un analizador lento baja los fps de analisis`() {
        // Con KEEP_ONLY_LATEST, si el análisis tarda 150 ms solo llega un fotograma de cada ~5.
        val medidor = MedidorRendimiento()
        repeat(8) { i -> medidor.registrar(timestampNs = i * 166_666_666L, duracionNs = 150 * ms) }

        val r = medidor.instantanea()
        assertEquals(6f, r.fps, 0.1f)
        assertEquals(150f, r.latenciaMaximaMs, 0.001f)
    }

    @Test
    fun `solo cuenta el ultimo segundo`() {
        val medidor = MedidorRendimiento()
        // 2 s a 10 fps con 40 ms de análisis y luego 1 s a 10 fps con 10 ms
        repeat(20) { i -> medidor.registrar(i * 100 * ms, 40 * ms) }
        repeat(10) { i -> medidor.registrar((2_000 + i * 100) * ms, 10 * ms) }

        val r = medidor.instantanea()
        assertEquals(10f, r.fps, 0.1f)
        // La ventana (≤ 1 s) contiene el último fotograma lento y los 10 rápidos.
        assertEquals(40f, r.latenciaMaximaMs, 0.001f)
        assertEquals(30L, r.analizados)
    }

    @Test
    fun `un salto atras en el tiempo reinicia la medicion`() {
        val medidor = MedidorRendimiento()
        repeat(10) { i -> medidor.registrar(5_000 * ms + i * 33 * ms, ms) }
        medidor.registrar(0, ms)

        val r = medidor.instantanea()
        assertEquals(0f, r.fps, 0f)
        assertEquals(1L, r.analizados)
    }
}
