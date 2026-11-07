package com.lhansqw.senaslsc.reconocimiento

import com.lhansqw.senaslsc.recognizer.Caracteristicas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcesadorCompartidoTest {

    private val modeloJson = """
        {
          "formato": 1,
          "nombre": "reconocedor-lsc",
          "version": "0.1.0",
          "lengua": "lsc",
          "caracteristicas": { "version": 1, "version_subconjuntos": 1, "por_fotograma": 352, "usa_rostro": false },
          "ventana": { "fotogramas": 32, "duracion_ms": 1000 },
          "salida": {
            "etiquetas": ["sin_sena", "lsc.hola", "lsc.gracias"],
            "etiqueta_sin_sena": "sin_sena",
            "temperatura": 1.0,
            "umbral_energia": null
          },
          "vocabulario": [
            { "id": "lsc.hola", "glosa": "HOLA", "significado": "hola", "tipo": "palabra" },
            { "id": "lsc.gracias", "glosa": "GRACIAS", "significado": "gracias", "tipo": "palabra" }
          ],
          "datos_entrenamiento": ["LSC70 (CC BY 4.0)"]
        }
    """.trimIndent()

    /** Modelo falso: con alguna mano en la ventana dice «HOLA» con mucha seguridad; si no, «sin seña». */
    private class InferenciaHola : InferenciaModelo {
        var llamadas = 0
        override fun ejecutar(entrada: FloatArray): FloatArray {
            llamadas++
            val hayMano = (0 until entrada.size / Caracteristicas.POR_FOTOGRAMA).any { f ->
                val base = f * Caracteristicas.POR_FOTOGRAMA
                entrada[base] == 1f || entrada[base + Caracteristicas.POR_MANO] == 1f
            }
            return if (hayMano) floatArrayOf(0f, 12f, 0f) else floatArrayOf(12f, 0f, 0f)
        }
    }

    private val manoDerecha = ManoDetectada(FloatArray(63) { i -> if (i % 3 == 2) 0f else 0.3f + i * 0.002f }, izquierdaSegunEtiqueta = false)
    private val pose = FloatArray(99) { i -> if (i % 3 == 2) 0f else 0.4f + (i % 7) * 0.03f }

    @Test
    fun `sin modelo el reloj muestra no disponible`() {
        val p = ProcesadorCompartido.crear(null, null)
        assertEquals(EstadoModeloSenas.SinModelo, p.estado.modelo)
        assertEquals(EstadoReloj.NO_DISPONIBLE, p.resumenReloj.estado)
        p.agregarFotograma(ConstructorFotograma.construir(0, listOf(manoDerecha), pose, 1f))
        assertTrue(p.estado.senas.isEmpty())
    }

    @Test
    fun `explica si falta una pieza o el json no vale`() {
        assertTrue(ProcesadorCompartido.crear(modeloJson, null).estado.modelo is EstadoModeloSenas.Invalido)
        assertTrue(ProcesadorCompartido.crear(null, InferenciaHola()).estado.modelo is EstadoModeloSenas.Invalido)
        assertTrue(ProcesadorCompartido.crear("{}", InferenciaHola()).estado.modelo is EstadoModeloSenas.Invalido)
    }

    @Test
    fun `si el modelo devuelve otro numero de salidas queda en fallo`() {
        val mala = object : InferenciaModelo {
            override fun ejecutar(entrada: FloatArray) = FloatArray(5)
        }
        val modelo = ProcesadorCompartido.crear(modeloJson, mala).estado.modelo
        assertTrue(modelo is EstadoModeloSenas.Fallo)
        assertTrue("5" in (modelo as EstadoModeloSenas.Fallo).mensaje)
    }

    @Test
    fun `reconoce una sena, cierra la frase con la pausa y la resume para el reloj`() {
        val inferencia = InferenciaHola()
        val p = ProcesadorCompartido.crear(modeloJson, inferencia)
        assertTrue(p.estado.modelo is EstadoModeloSenas.Listo)
        assertEquals(EstadoReloj.RECONOCIENDO, p.resumenReloj.estado)

        var t = 1_000L
        repeat(90) { // 3 s haciendo la seña a 30 FPS
            p.agregarFotograma(ConstructorFotograma.construir(t, listOf(manoDerecha), pose, 0.75f))
            t += 33
        }
        assertTrue(inferencia.llamadas > 1)
        assertEquals("HOLA", p.estado.senas.firstOrNull()?.entrada?.glosa)
        assertEquals("HOLA", p.resumenReloj.ultimaSena)

        repeat(120) { // 4 s con las manos bajadas
            p.agregarFotograma(ConstructorFotograma.construir(t, emptyList(), pose, 0.75f))
            t += 33
        }
        val resumen = p.resumenReloj
        assertTrue(p.estado.secuenciaCerrada)
        assertEquals(EstadoReloj.FRASE_LISTA, resumen.estado)
        assertEquals(p.estado.propuesta!!.texto, resumen.frase)
        assertEquals(p.estado.senas.size, resumen.numeroSenas)

        assertTrue(p.corregir("Hola a todos"))
        assertEquals("Hola a todos", p.resumenReloj.frase)
        p.limpiar()
        assertEquals(0, p.resumenReloj.numeroSenas)
        assertFalse(p.corregir("x"))
    }
}
