package com.lhansqw.senaslsc.camara

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class GeometriaTest {

    @Test
    fun `girar 90 o 270 grados intercambia ancho y alto`() {
        assertEquals(480 to 640, dimensionesGiradas(640, 480, 90))
        assertEquals(480 to 640, dimensionesGiradas(640, 480, 270))
        assertEquals(640 to 480, dimensionesGiradas(640, 480, 0))
        assertEquals(640 to 480, dimensionesGiradas(640, 480, 180))
        try {
            dimensionesGiradas(640, 480, 45)
            fail("Se esperaba IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `una vista mas alta que la imagen recorta los lados`() {
        // Imagen 480×640 (3:4) en una pantalla 1080×2400: la escala la da el alto (2400/640 = 3,75)
        // y la imagen escalada mide 1800 de ancho, así que se recortan 360 px por cada lado.
        val t = TransformacionVista(480, 640, 1080f, 2400f, espejo = false)

        assertEquals(-360f, t.x(0f), 0.01f)
        assertEquals(540f, t.x(0.5f), 0.01f)   // el centro queda en el centro
        assertEquals(1440f, t.x(1f), 0.01f)
        assertEquals(0f, t.y(0f), 0.01f)
        assertEquals(2400f, t.y(1f), 0.01f)
    }

    @Test
    fun `con espejo la izquierda de la imagen se dibuja a la derecha`() {
        val t = TransformacionVista(480, 640, 480f, 640f, espejo = true)

        assertEquals(480f, t.x(0f), 0.01f)
        assertEquals(0f, t.x(1f), 0.01f)
        assertEquals(120f, t.x(0.75f), 0.01f)
    }

    @Test
    fun `una vista mas ancha que la imagen recorta arriba y abajo`() {
        val t = TransformacionVista(480, 640, 960f, 640f, espejo = false)  // escala 2

        assertEquals(0f, t.x(0f), 0.01f)
        assertEquals(960f, t.x(1f), 0.01f)
        assertEquals(-320f, t.y(0f), 0.01f)
        assertEquals(320f, t.y(0.5f), 0.01f)
    }
}
