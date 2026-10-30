package com.lhansqw.senaslsc.vocabulario

import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.TipoSena
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltroVocabularioTest {

    private fun e(id: String, glosa: String, significado: String, tipo: TipoSena) = EntradaVocabulario(id, glosa, significado, tipo)

    private val vocabulario = listOf(
        e("lsc.gracias", "GRACIAS", "gracias", TipoSena.PALABRA),
        e("lsc.como", "CÓMO", "cómo", TipoSena.PALABRA),
        e("lsc.adios", "ADIÓS", "adiós", TipoSena.PALABRA),
        e("lsc.n10", "10", "10", TipoSena.NUMERO),
        e("lsc.n2", "2", "2", TipoSena.NUMERO),
        e("lsc.b", "B", "b", TipoSena.LETRA),
        e("lsc.a", "A", "a", TipoSena.LETRA),
    )

    @Test
    fun `agrupa en orden fijo y ordena dentro de cada grupo`() {
        val grupos = filtrarVocabulario(vocabulario)

        assertEquals(listOf(TipoSena.PALABRA, TipoSena.NUMERO, TipoSena.LETRA), grupos.map { it.tipo })
        assertEquals(listOf("adiós", "cómo", "gracias"), grupos[0].entradas.map { it.significado })
        assertEquals(listOf("2", "10"), grupos[1].entradas.map { it.significado })   // por valor, no como texto
        assertEquals(listOf("a", "b"), grupos[2].entradas.map { it.significado })
    }

    @Test
    fun `busca sin tildes en glosa y significado`() {
        assertEquals(listOf("lsc.como"), filtrarVocabulario(vocabulario, "COMO").flatMap { it.entradas }.map { it.id })
        assertEquals(listOf("lsc.adios"), filtrarVocabulario(vocabulario, "adios").flatMap { it.entradas }.map { it.id })
    }

    @Test
    fun `filtra por tipo y omite los grupos vacios`() {
        val letras = filtrarVocabulario(vocabulario, tipo = TipoSena.LETRA)
        assertEquals(1, letras.size)
        assertTrue(filtrarVocabulario(vocabulario, "zzz").isEmpty())
    }
}
