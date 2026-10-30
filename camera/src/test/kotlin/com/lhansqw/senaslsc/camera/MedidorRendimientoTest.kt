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

    @Test
    fun `p50 y p95 de las ultimas mediciones`() {
        val medidor = MedidorRendimiento()
        // 95 fotogramas de 10 ms y 5 de 60 ms (tirones): la media apenas sube, el p95 sí.
        repeat(100) { i -> medidor.registrar(timestampNs = i * 33_333_333L, duracionNs = (if (i % 20 == 19) 60 else 10) * ms) }
        val r = medidor.instantanea()
        assertEquals(10f, r.latenciaP50Ms, 0.001f)
        assertEquals(10f, r.latenciaP95Ms, 0.001f)
        medidor.registrar(timestampNs = 100 * 33_333_333L, duracionNs = 60 * ms)
        assertEquals(60f, medidor.instantanea().latenciaP95Ms, 0.001f)  // 6 de 101 superan el 5 %
    }

    @Test
    fun `los percentiles usan mas que el ultimo segundo y se borran al reiniciar`() {
        val medidor = MedidorRendimiento(muestrasPercentil = 50)
        repeat(40) { i -> medidor.registrar(timestampNs = i * 100_000_000L, duracionNs = 80 * ms) }   // 4 s lentos
        repeat(12) { i -> medidor.registrar(timestampNs = (40 + i) * 100_000_000L, duracionNs = 5 * ms) } // 1,2 s rápido
        val r = medidor.instantanea()
        assertEquals(5f, r.latenciaMaximaMs, 0.001f)   // la ventana de 1 s ya solo ve los rápidos
        assertEquals(80f, r.latenciaP50Ms, 0.001f)     // los percentiles recuerdan los 5 s
        medidor.reiniciar()
        assertEquals(Rendimiento.VACIO, medidor.instantanea())
    }
}
