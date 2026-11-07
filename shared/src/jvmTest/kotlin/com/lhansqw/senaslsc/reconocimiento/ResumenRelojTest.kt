package com.lhansqw.senaslsc.reconocimiento

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResumenRelojTest {

    @Test
    fun `el diccionario va y vuelve sin perder nada`() {
        val r = ResumenReloj(EstadoReloj.FRASE_LISTA, "Hola, buenas noches.", "NOCHES", 3, incierta = true)
        assertEquals(r, ResumenReloj.deMapa(r.aMapa()))

        val sinSena = r.copy(ultimaSena = null, incierta = false)
        assertEquals(sinSena, ResumenReloj.deMapa(sinSena.aMapa()))
    }

    @Test
    fun `un diccionario sin estado valido se ignora`() {
        assertNull(ResumenReloj.deMapa(mapOf("frase" to "hola")))
        assertNull(ResumenReloj.deMapa(mapOf("estado" to "OTRO")))
    }

    @Test
    fun `mientras carga el modelo el reloj dice preparando`() {
        assertEquals(EstadoReloj.PREPARANDO, ResumenReloj.de(EstadoReconocimiento()).estado)
        assertEquals("", ResumenReloj.de(EstadoReconocimiento()).frase)
    }
}
