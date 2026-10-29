package com.lhansqw.senaslsc.flujo

import com.lhansqw.senaslsc.core.model.Candidata
import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.PrediccionSena
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.core.model.Vocabulario
import com.lhansqw.senaslsc.interpreter.rules.InterpreteReglas
import com.lhansqw.senaslsc.sequence.ConstructorSecuencia
import com.lhansqw.senaslsc.sequence.FiltroTemporal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Recorre las etapas 4 a 7 de la arquitectura con predicciones simuladas:
 * predicciones del modelo → filtro temporal → secuencia → intérprete por reglas.
 * Comprueba que los módulos encajan sin depender de la cámara ni del modelo real.
 */
class FlujoSimuladoTest {

    private val vocabulario = Vocabulario(
        "lsc",
        listOf(
            EntradaVocabulario("lsc.hola", "HOLA", "hola", TipoSena.PALABRA),
            EntradaVocabulario("lsc.como", "COMO", "cómo", TipoSena.PALABRA, interrogativa = true),
            EntradaVocabulario("lsc.estar", "ESTAR", "estar", TipoSena.PALABRA),
        ),
    )

    private var indice = 0

    private fun ventanas(id: String?, cuantas: Int, probabilidad: Float = 0.9f): List<PrediccionSena> =
        List(cuantas) {
            val inicio = indice++ * 333L
            if (id == null) {
                PrediccionSena(inicio, inicio + 1_000, listOf(Candidata("lsc.hola", 0.2f)), desconocida = true)
            } else {
                PrediccionSena(inicio, inicio + 1_000, listOf(Candidata(id, probabilidad)), desconocida = false)
            }
        }

    @Test
    fun `de predicciones crudas a una propuesta de frase`() {
        val filtro = FiltroTemporal(vocabulario)
        val secuencia = ConstructorSecuencia(pausaCierreMs = 1_200)

        val predicciones = ventanas("lsc.hola", 4) +
            ventanas(null, 1) +                    // transición entre señas
            ventanas("lsc.como", 3, probabilidad = 0.72f) +
            ventanas("lsc.estar", 2) +             // solo 2 ventanas: no se confirma
            ventanas(null, 2)

        for (p in predicciones) {
            filtro.procesar(p)?.let { secuencia.agregar(it) }
            secuencia.actualizar(p.ventanaFinMs, hayManos = !p.desconocida)
        }
        val fin = predicciones.last().ventanaFinMs
        assertTrue(secuencia.actualizar(fin + 1_500, hayManos = false))

        assertEquals(listOf("lsc.hola", "lsc.como"), secuencia.actual.map { it.id })

        val propuesta = InterpreteReglas().componer(secuencia.actual)
        assertEquals("¿Hola cómo?", propuesta.texto)
        assertTrue(propuesta.incierta)                       // COMO con confianza 0,72 < 0,75
        assertFalse("lsc.estar" in propuesta.senasUsadas)    // nunca aparece lo no confirmado
    }
}
