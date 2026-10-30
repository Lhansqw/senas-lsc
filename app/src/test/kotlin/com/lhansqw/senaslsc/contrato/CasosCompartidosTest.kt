package com.lhansqw.senaslsc.contrato

import com.lhansqw.senaslsc.core.json.Json
import com.lhansqw.senaslsc.core.json.entero
import com.lhansqw.senaslsc.core.json.lista
import com.lhansqw.senaslsc.recognizer.Caracteristicas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * La app sigue calculando lo mismo que quedó registrado en los casos compartidos con Python. Si
 * esta prueba falla, el contrato del modelo cambió: hay que subir su versión, regenerar los casos
 * y reentrenar.
 */
class CasosCompartidosTest {

    private val raiz = CasosCompartidos.leerRaiz()

    @Test
    fun `los casos son de la version actual de las caracteristicas`() {
        assertEquals(Caracteristicas.VERSION.toLong(), raiz.entero("version_caracteristicas"))
        assertTrue(CasosCompartidos.casos(raiz).size >= 4)
    }

    @Test
    fun `kotlin reproduce las salidas registradas`() {
        for ((caso, json) in CasosCompartidos.casos(raiz)) {
            val (tiempos, caracteristicas) = CasosCompartidos.calcular(caso)
            val esperadosT = json.lista("esperado_timestamps").map { (it as Json.Numero).valor.toLong() }
            assertEquals("timestamps de ${caso.nombre}", esperadosT, tiempos)

            val filas = json.lista("esperado_caracteristicas")
            assertEquals(tiempos.size, filas.size)
            filas.forEachIndexed { i, fila ->
                val valores = (fila as Json.Lista).elementos
                assertEquals(Caracteristicas.POR_FOTOGRAMA, valores.size)
                valores.forEachIndexed { k, v ->
                    val esperado = (v as Json.Numero).valor
                    val actual = caracteristicas[i * Caracteristicas.POR_FOTOGRAMA + k].toDouble()
                    assertTrue("${caso.nombre}, fotograma $i, valor $k: $esperado ≠ $actual", abs(esperado - actual) <= 1e-5 * maxOf(1.0, abs(esperado)))
                }
            }
        }
    }
}
