package com.lhansqw.senaslsc.recognizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.exp
import kotlin.math.ln

class PostProcesadorTest {

    private val etiquetas = listOf("sin_sena", "lsc.hola", "lsc.gracias", "lsc.adios")

    @Test
    fun `aplica softmax y ordena las candidatas sin la clase sin sena`() {
        val p = PostProcesador(etiquetas, "sin_sena").aplicar(floatArrayOf(0f, 3f, 1f, 0f), 0, 1_000)

        assertEquals(listOf("lsc.hola", "lsc.gracias", "lsc.adios"), p.candidatas.map { it.id })
        val total = exp(0.0) + exp(3.0) + exp(1.0) + exp(0.0)
        assertEquals((exp(3.0) / total).toFloat(), p.candidatas[0].probabilidad, 1e-5f)
        assertFalse(p.desconocida)
    }

    @Test
    fun `si gana la clase sin sena la entrada es desconocida`() {
        val p = PostProcesador(etiquetas, "sin_sena").aplicar(floatArrayOf(5f, 1f, 0f, 0f), 0, 1_000)
        assertTrue(p.desconocida)
        assertEquals("lsc.hola", p.mejor?.id)  // las candidatas se informan igual, con poca probabilidad
    }

    @Test
    fun `la temperatura suaviza las probabilidades`() {
        val logits = floatArrayOf(0f, 4f, 2f, 0f)
        val fria = PostProcesador(etiquetas, "sin_sena", temperatura = 1f).aplicar(logits, 0, 1)
        val caliente = PostProcesador(etiquetas, "sin_sena", temperatura = 2f).aplicar(logits, 0, 1)
        assertTrue(caliente.mejor!!.probabilidad < fria.mejor!!.probabilidad)
    }

    @Test
    fun `la energia alta marca la entrada como desconocida`() {
        val pp = PostProcesador(etiquetas, null, umbralEnergia = -3f)
        // Logits bajos y parecidos: ninguna clase encaja → energía ≈ −(0,5 + ln 4) ≈ −1,89 > −3.
        val dudosa = pp.aplicar(floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f), 0, 1)
        assertEquals((-(0.5 + ln(4.0))).toFloat(), pp.energia(floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)), 1e-5f)
        assertTrue(dudosa.desconocida)

        // Un logit muy alto: energía ≈ −8 < −3 → conocida.
        val clara = pp.aplicar(floatArrayOf(0f, 8f, 0f, 0f), 0, 1)
        assertFalse(clara.desconocida)
    }

    @Test
    fun `limita el numero de candidatas`() {
        val p = PostProcesador(etiquetas, "sin_sena", maximoCandidatas = 1).aplicar(floatArrayOf(0f, 1f, 2f, 3f), 0, 1)
        assertEquals(listOf("lsc.adios"), p.candidatas.map { it.id })
    }

    @Test
    fun `es estable con logits muy grandes`() {
        val p = PostProcesador(etiquetas, "sin_sena").aplicar(floatArrayOf(0f, 1000f, 999f, 0f), 0, 1)
        assertEquals((1 / (1 + exp(-1.0))).toFloat(), p.mejor!!.probabilidad, 1e-5f)
    }

    @Test
    fun `rechaza salidas de otro tamano o no finitas`() {
        val pp = PostProcesador(etiquetas, "sin_sena")
        listOf(floatArrayOf(1f, 2f), floatArrayOf(0f, Float.NaN, 0f, 0f)).forEach { logits ->
            try {
                pp.aplicar(logits, 0, 1)
                fail("Se esperaba IllegalArgumentException")
            } catch (_: IllegalArgumentException) {
            }
        }
    }
}
