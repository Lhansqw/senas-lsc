package com.lhansqw.senaslsc.recognizer

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.Punto3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaracteristicasTest {

    private val C = Caracteristicas

    // Posiciones de cada bloque dentro del vector de un fotograma.
    private val IZQ = 0
    private val DER = C.POR_MANO
    private val CUERPO = 2 * C.POR_MANO
    private val ROSTRO = CUERPO + C.POR_CUERPO
    private val VEL = ROSTRO + C.POR_ROSTRO

    /** Cuerpo con los hombros en (0,4; 0,5) y (0,6; 0,5): origen (0,5; 0,5) y escala 0,2. */
    private fun cuerpo() = listOf(
        Punto3D(0.5f, 0.3f, 0f),   // nariz
        Punto3D(0.4f, 0.5f, 0f),   // hombro izquierdo
        Punto3D(0.6f, 0.5f, 0f),   // hombro derecho
        Punto3D(0.35f, 0.7f, 0f), Punto3D(0.65f, 0.7f, 0f),
        Punto3D(0.3f, 0.9f, 0f), Punto3D(0.7f, 0.9f, 0f),
    )

    /** Mano con la muñeca en (x, y) y la base del dedo medio 0,1 más arriba. */
    private fun mano(x: Float, y: Float) = List(21) { i ->
        if (i == 9) Punto3D(x, y - 0.1f, 0f) else Punto3D(x, y, 0f)
    }

    private fun frame(
        ms: Long = 0,
        izq: List<Punto3D>? = null,
        der: List<Punto3D>? = null,
        cuerpo: List<Punto3D>? = cuerpo(),
        rostro: List<Punto3D>? = null,
        ar: Float = 1f,
    ) = LandmarkFrame(ms, izq, der, cuerpo, rostro, ar)

    @Test
    fun `el vector de un fotograma tiene 352 valores`() {
        assertEquals(127, C.POR_MANO)
        assertEquals(15, C.POR_CUERPO)
        assertEquals(79, C.POR_ROSTRO)
        assertEquals(352, C.POR_FOTOGRAMA)
        assertEquals(3 * 352, C.deVentana(List(3) { frame(it * 33L) }).size)
    }

    @Test
    fun `sin detecciones todo vale cero`() {
        val v = C.deVentana(listOf(frame(cuerpo = null)))
        assertTrue(v.all { it == 0f })
    }

    @Test
    fun `la referencia es el punto medio de los hombros y su distancia`() {
        val ref = C.referencia(frame())
        assertEquals(0.5f, ref.origenX, 1e-6f)
        assertEquals(0.5f, ref.origenY, 1e-6f)
        assertEquals(0.2f, ref.escala, 1e-6f)
    }

    @Test
    fun `sin cuerpo se usa el centro de la imagen`() {
        val ref = C.referencia(frame(cuerpo = null, ar = 0.75f))
        assertEquals(0.375f, ref.origenX, 1e-6f)
        assertEquals(0.5f, ref.origenY, 1e-6f)
        assertEquals(C.ESCALA_SIN_CUERPO, ref.escala, 0f)
    }

    @Test
    fun `la posicion de la mano es relativa al cuerpo`() {
        val v = C.deVentana(listOf(frame(izq = mano(0.7f, 0.6f))))

        assertEquals(1f, v[IZQ], 0f)                 // presencia
        assertEquals(1f, v[IZQ + 1], 1e-5f)          // (0,7 − 0,5) / 0,2
        assertEquals(0.5f, v[IZQ + 2], 1e-5f)        // (0,6 − 0,5) / 0,2
        assertEquals(0f, v[DER], 0f)                 // mano derecha ausente
    }

    @Test
    fun `la forma de la mano no depende de donde este ni de su tamano en la imagen`() {
        val cerca = C.deVentana(listOf(frame(izq = mano(0.3f, 0.6f))))
        val lejos = C.deVentana(listOf(frame(izq = mano(0.8f, 0.4f))))
        val inicioForma = IZQ + 1 + 21 * 3

        for (k in inicioForma until inicioForma + 21 * 3) assertEquals(cerca[k], lejos[k], 1e-5f)
        // La base del dedo medio queda a una unidad por encima de la muñeca.
        assertEquals(-1f, cerca[inicioForma + 9 * 3 + 1], 1e-5f)
    }

    @Test
    fun `alejarse de la camara no cambia las caracteristicas`() {
        // La misma escena a la mitad de tamaño y desplazada: todo es relativo a los hombros.
        fun escalar(p: Punto3D) = Punto3D(0.25f + p.x / 2, 0.25f + p.y / 2, p.z / 2)
        val normal = frame(izq = mano(0.7f, 0.6f))
        val lejos = frame(izq = mano(0.7f, 0.6f).map(::escalar), cuerpo = cuerpo().map(::escalar))

        val a = C.deVentana(listOf(normal))
        val b = C.deVentana(listOf(lejos))
        for (k in a.indices) assertEquals("posición $k", a[k], b[k], 1e-4f)
    }

    @Test
    fun `la relacion de aspecto corrige las distancias horizontales`() {
        // En una imagen 480×640 (ar = 0,75) los hombros a 0,2 de distancia en x miden 0,15 de alto.
        assertEquals(0.15f, C.referencia(frame(ar = 0.75f)).escala, 1e-6f)
    }

    @Test
    fun `cuerpo y rostro escriben presencia y coordenadas`() {
        val rostro = List(39) { Punto3D(0.5f, 0.3f, 0f) }
        val v = C.deVentana(listOf(frame(rostro = rostro)))

        assertEquals(1f, v[CUERPO], 0f)
        assertEquals(0f, v[CUERPO + 1], 1e-5f)       // nariz x: (0,5 − 0,5) / 0,2
        assertEquals(-1f, v[CUERPO + 2], 1e-5f)      // nariz y: (0,3 − 0,5) / 0,2
        assertEquals(1f, v[ROSTRO], 0f)
        assertEquals(-1f, v[ROSTRO + 2], 1e-5f)
    }

    @Test
    fun `sin usar el rostro su bloque queda a cero y el resto no cambia`() {
        val rostro = List(39) { Punto3D(0.5f, 0.3f, 0f) }
        val ventana = listOf(frame(0, izq = mano(0.7f, 0.6f), rostro = rostro), frame(33, izq = mano(0.71f, 0.6f), rostro = rostro))
        val con = C.deVentana(ventana)
        val sin = C.deVentana(ventana, usarRostro = false)

        assertEquals(1f, con[ROSTRO], 0f)
        for (f in 0..1) {
            val desde = f * C.POR_FOTOGRAMA
            for (k in ROSTRO until VEL) assertEquals(0f, sin[desde + k], 0f)
        }
        // Igual que si MediaPipe no hubiera detectado la cara.
        val sinDeteccion = C.deVentana(ventana.map { it.copy(rostro = null) })
        assertTrue(sin.contentEquals(sinDeteccion))
    }

    @Test
    fun `subconjuntos de otro tamano se tratan como ausentes`() {
        val v = C.deVentana(listOf(frame(cuerpo = cuerpo().take(5), rostro = List(10) { Punto3D(0f, 0f, 0f) })))
        assertEquals(0f, v[CUERPO], 0f)
        assertEquals(0f, v[ROSTRO], 0f)
    }

    @Test
    fun `la velocidad de la muneca se mide por segundo`() {
        // La muñeca izquierda se mueve 0,02 en x (0,1 unidades de referencia) en 100 ms → 1 por segundo.
        val ventana = listOf(frame(0, izq = mano(0.70f, 0.6f)), frame(100, izq = mano(0.72f, 0.6f)))
        val v = C.deVentana(ventana)

        assertEquals(0f, v[VEL], 0f)                                  // primer fotograma: sin anterior
        assertEquals(1f, v[C.POR_FOTOGRAMA + VEL], 1e-4f)            // vx izquierda
        assertEquals(0f, v[C.POR_FOTOGRAMA + VEL + 1], 1e-4f)        // vy izquierda
        assertEquals(0f, v[C.POR_FOTOGRAMA + VEL + 2], 0f)           // derecha ausente
    }

    @Test
    fun `sin la mano en el fotograma anterior la velocidad es cero`() {
        val v = C.deVentana(listOf(frame(0), frame(100, izq = mano(0.7f, 0.6f))))
        assertEquals(0f, v[C.POR_FOTOGRAMA + VEL], 0f)
    }
}
