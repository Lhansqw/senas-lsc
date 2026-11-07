package com.lhansqw.senaslsc.sequence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FiltroTemporalTest {

    private fun filtro() = FiltroTemporal(vocabularioPrueba, ConfiguracionFiltro(ventanasParaConfirmar = 3, ventanasParaLiberar = 2))

    @Test
    fun `confirma tras tres ventanas seguidas con la misma sena`() {
        val filtro = filtro()

        assertNull(filtro.procesar(prediccion(0, "lsc.hola", 0.90f)))
        assertNull(filtro.procesar(prediccion(1, "lsc.hola", 0.80f)))
        val sena = filtro.procesar(prediccion(2, "lsc.hola", 0.85f))

        assertNotNull(sena)
        assertEquals("lsc.hola", sena!!.id)
        assertEquals(0.85f, sena.confianza, 1e-4f)
        assertEquals(0L, sena.inicioMs)             // inicio de la primera ventana de la racha
        assertEquals(2 * 333L + 1_000, sena.finMs)  // fin de la última
    }

    @Test
    fun `una ventana de baja confianza interrumpe la racha`() {
        val filtro = filtro()
        filtro.procesar(prediccion(0, "lsc.hola", 0.9f))
        filtro.procesar(prediccion(1, "lsc.hola", 0.9f))
        assertNull(filtro.procesar(prediccion(2, "lsc.hola", 0.5f)))

        assertNull(filtro.procesar(prediccion(3, "lsc.hola", 0.9f)))
        assertNull(filtro.procesar(prediccion(4, "lsc.hola", 0.9f)))
        assertNotNull(filtro.procesar(prediccion(5, "lsc.hola", 0.9f)))
    }

    @Test
    fun `una prediccion ambigua no cuenta aunque la confianza sea alta`() {
        val filtro = filtro()
        repeat(5) { i ->
            assertNull(filtro.procesar(prediccion(i, "lsc.hola", 0.75f, segunda = "lsc.gracias" to 0.70f)))
        }
    }

    @Test
    fun `una ventana marcada como desconocida interrumpe la racha`() {
        val filtro = filtro()
        filtro.procesar(prediccion(0, "lsc.hola", 0.9f))
        filtro.procesar(prediccion(1, "lsc.hola", 0.9f))
        assertNull(filtro.procesar(desconocida(2)))
        assertNull(filtro.procesar(prediccion(3, "lsc.hola", 0.9f)))
    }

    @Test
    fun `ignora identificadores que no estan en el vocabulario`() {
        val filtro = filtro()
        repeat(5) { i -> assertNull(filtro.procesar(prediccion(i, "lsc.adios", 0.99f))) }
    }

    @Test
    fun `mantener la sena no la repite`() {
        val filtro = filtro()
        val confirmadas = (0 until 12).mapNotNull { filtro.procesar(prediccion(it, "lsc.hola", 0.9f)) }
        assertEquals(1, confirmadas.size)
    }

    @Test
    fun `bajar la mano permite repetir la misma sena`() {
        val filtro = filtro()
        val resultados = mutableListOf<String>()
        var i = 0
        repeat(3) { filtro.procesar(prediccion(i++, "lsc.l", 0.9f))?.let { resultados += it.id } }
        repeat(2) { filtro.procesar(desconocida(i++)) }  // ventanasParaLiberar = 2
        repeat(3) { filtro.procesar(prediccion(i++, "lsc.l", 0.9f))?.let { resultados += it.id } }

        assertEquals(listOf("lsc.l", "lsc.l"), resultados)
    }

    @Test
    fun `una sola ventana de ruido no libera la sena sostenida`() {
        val filtro = filtro()
        var i = 0
        val confirmadas = mutableListOf<String>()
        repeat(3) { filtro.procesar(prediccion(i++, "lsc.hola", 0.9f))?.let { confirmadas += it.id } }
        filtro.procesar(desconocida(i++))
        repeat(4) { filtro.procesar(prediccion(i++, "lsc.hola", 0.9f))?.let { confirmadas += it.id } }

        assertEquals(listOf("lsc.hola"), confirmadas)
    }

    @Test
    fun `una sena distinta se confirma sin esperar la liberacion`() {
        val filtro = filtro()
        var i = 0
        val confirmadas = mutableListOf<String>()
        repeat(3) { filtro.procesar(prediccion(i++, "lsc.hola", 0.9f))?.let { confirmadas += it.id } }
        repeat(3) { filtro.procesar(prediccion(i++, "lsc.gracias", 0.9f))?.let { confirmadas += it.id } }

        assertEquals(listOf("lsc.hola", "lsc.gracias"), confirmadas)
    }

    @Test
    fun `reiniciar olvida la racha en curso`() {
        val filtro = filtro()
        filtro.procesar(prediccion(0, "lsc.hola", 0.9f))
        filtro.procesar(prediccion(1, "lsc.hola", 0.9f))
        filtro.reiniciar()
        assertNull(filtro.procesar(prediccion(2, "lsc.hola", 0.9f)))
    }
}
