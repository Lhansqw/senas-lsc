package com.lhansqw.senaslsc.recognizer

import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.TipoSena
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContratoModeloTest {

    private fun entrada(id: String) = EntradaVocabulario(id, id.substringAfter('.').uppercase(), id.substringAfter('.'), TipoSena.PALABRA)

    private fun contrato(
        versionCaracteristicas: Int = Caracteristicas.VERSION,
        porFotograma: Int = Caracteristicas.POR_FOTOGRAMA,
        etiquetas: List<String> = listOf("sin_sena", "lsc.hola", "lsc.gracias"),
        sinSena: String? = "sin_sena",
        entradas: List<EntradaVocabulario> = listOf(entrada("lsc.hola"), entrada("lsc.gracias")),
        temperatura: Float = 1.3f,
    ) = ContratoModelo(
        nombre = "reconocedor-lsc",
        version = "0.1.0",
        lengua = "lsc",
        versionCaracteristicas = versionCaracteristicas,
        versionSubconjuntos = Caracteristicas.VERSION_SUBCONJUNTOS,
        fotogramas = 32,
        duracionVentanaMs = 1_000,
        caracteristicasPorFotograma = porFotograma,
        etiquetas = etiquetas,
        etiquetaSinSena = sinSena,
        temperatura = temperatura,
        umbralEnergia = -2.5f,
        entradas = entradas,
        datosEntrenamiento = listOf("LSC70 (CC BY 4.0)"),
    )

    @Test
    fun `un contrato correcto no tiene problemas`() {
        val c = contrato()
        assertEquals(emptyList<String>(), c.validar())
        assertEquals(2, c.vocabulario().tamano)
        assertEquals("reconocedor-lsc", c.ficha().nombre)
    }

    @Test
    fun `rechaza otra version de caracteristicas o de tamano`() {
        assertTrue(contrato(versionCaracteristicas = 2).validar().any { "características" in it })
        assertTrue(contrato(porFotograma = 300).validar().any { "300" in it })
    }

    @Test
    fun `cada etiqueta necesita su entrada de vocabulario y al reves`() {
        assertTrue(contrato(entradas = listOf(entrada("lsc.hola"))).validar().any { "lsc.gracias" in it })
        assertTrue(
            contrato(entradas = listOf(entrada("lsc.hola"), entrada("lsc.gracias"), entrada("lsc.adios")))
                .validar().any { "lsc.adios" in it },
        )
    }

    @Test
    fun `la clase sin sena debe estar entre las etiquetas`() {
        assertTrue(contrato(sinSena = "ninguna").validar().any { "sin seña" in it })
        assertEquals(emptyList<String>(), contrato(etiquetas = listOf("lsc.hola", "lsc.gracias"), sinSena = null).validar())
    }

    @Test
    fun `rechaza etiquetas repetidas y temperatura no valida`() {
        assertTrue(contrato(etiquetas = listOf("sin_sena", "lsc.hola", "lsc.hola", "lsc.gracias")).validar().any { "repetidas" in it })
        assertTrue(contrato(temperatura = 0f).validar().any { "temperatura" in it })
        assertTrue(contrato(temperatura = Float.NaN).validar().any { "temperatura" in it })
    }
}
