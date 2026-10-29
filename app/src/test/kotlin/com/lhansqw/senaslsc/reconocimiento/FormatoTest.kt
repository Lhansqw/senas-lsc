package com.lhansqw.senaslsc.reconocimiento

import com.lhansqw.senaslsc.core.model.Candidata
import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.PrediccionSena
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.core.model.TipoSena
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatoTest {

    private val hola = EntradaVocabulario("lsc.hola", "HOLA", "hola", TipoSena.PALABRA)
    private val gracias = EntradaVocabulario("lsc.gracias", "GRACIAS", "gracias", TipoSena.PALABRA)
    private val glosas = mapOf("lsc.hola" to "HOLA")

    private fun prediccion(id: String, p: Float, desconocida: Boolean = false) =
        PrediccionSena(0, 100, listOf(Candidata(id, p)), desconocida)

    @Test
    fun `porcentaje redondeado con espacio y acotado`() {
        assertEquals("82 %", formatearPorcentaje(0.823f))
        assertEquals("100 %", formatearPorcentaje(1.2f))
        assertEquals("0 %", formatearPorcentaje(-0.1f))
    }

    @Test
    fun `etiqueta de una sena confirmada`() {
        assertEquals("HOLA 91 %", etiquetaSena(SenaConfirmada(hola, 0.91f, 0, 10)))
    }

    @Test
    fun `prediccion en curso solo si es conocida y esta en el vocabulario`() {
        assertEquals("HOLA 75 %", textoPrediccionEnCurso(prediccion("lsc.hola", 0.75f)) { glosas[it] })
        assertNull(textoPrediccionEnCurso(prediccion("lsc.hola", 0.75f, desconocida = true)) { glosas[it] })
        assertNull(textoPrediccionEnCurso(prediccion("lsc.otra", 0.9f)) { glosas[it] })
        assertNull(textoPrediccionEnCurso(null) { glosas[it] })
    }

    @Test
    fun `lista para lector de pantalla`() {
        assertEquals("HOLA, GRACIAS", listaParaLector(listOf(SenaConfirmada(hola, 0.9f, 0, 1), SenaConfirmada(gracias, 0.8f, 2, 3))))
    }

    @Test
    fun `resumen de metricas en orden fijo e ignorando las desconocidas`() {
        assertEquals(
            "Exactitud 86 % · F1 macro 81 %",
            resumenMetricas(mapOf("macro_f1" to 0.81f, "otra" to 0.5f, "exactitud" to 0.86f)),
        )
        assertNull(resumenMetricas(emptyMap()))
    }
}
