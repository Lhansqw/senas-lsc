package com.lhansqw.senaslsc.core.descargas

import com.lhansqw.senaslsc.core.descargas.Reanudacion.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReanudacionTest {

    @Test
    fun `lee content-range con y sin total`() {
        assertEquals(RangoContenido(100, 999, 1_000), Reanudacion.leerRango("bytes 100-999/1000"))
        assertEquals(RangoContenido(0, 9, null), Reanudacion.leerRango("bytes 0-9/*"))
        assertNull(Reanudacion.leerRango("bytes 10-5/100"))
        assertNull(Reanudacion.leerRango("bytes 0-100/100"))
        assertNull(Reanudacion.leerRango("items 0-9/10"))
        assertNull(Reanudacion.leerRango(null))
    }

    @Test
    fun `cabecera range solo si hay bytes descargados`() {
        assertEquals("bytes=500-", Reanudacion.cabeceraRango(500))
        assertNull(Reanudacion.cabeceraRango(0))
    }

    @Test
    fun `200 empieza de cero aunque se pidiera continuar`() {
        assertEquals(Decision.EmpezarDeCero(total = 1_000), Reanudacion.decidir(500, 200, null, 1_000))
        assertEquals(Decision.EmpezarDeCero(total = null), Reanudacion.decidir(0, 200, null, -1))
    }

    @Test
    fun `206 continua solo si el servidor sigue en el byte pedido`() {
        assertEquals(Decision.Continuar(500, 1_000), Reanudacion.decidir(500, 206, "bytes 500-999/1000", 500))
        assertTrue(Reanudacion.decidir(500, 206, "bytes 0-999/1000", 1_000) is Decision.Fallo)
        assertTrue(Reanudacion.decidir(500, 206, "basura", 500) is Decision.Fallo)
    }

    @Test
    fun `416 con el archivo completo no vuelve a descargar`() {
        assertEquals(Decision.YaCompleto(1_000), Reanudacion.decidir(1_000, 416, "bytes */1000", 0))
        assertTrue(Reanudacion.decidir(1_200, 416, "bytes */1000", 0) is Decision.EmpezarDeCero)
    }

    @Test
    fun `errores del cliente y del servidor con mensajes claros`() {
        val prohibido = Reanudacion.decidir(0, 403, null, -1) as Decision.Fallo
        assertTrue(prohibido.mensaje.contains("licencia"))
        assertEquals(false, prohibido.reintentable)

        val noEncontrado = Reanudacion.decidir(0, 404, null, -1) as Decision.Fallo
        assertTrue(noEncontrado.mensaje.contains("404"))

        val servidor = Reanudacion.decidir(0, 503, null, -1) as Decision.Fallo
        assertTrue(servidor.reintentable)
    }
}
