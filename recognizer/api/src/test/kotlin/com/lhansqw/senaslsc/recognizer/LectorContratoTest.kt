package com.lhansqw.senaslsc.recognizer

import com.lhansqw.senaslsc.core.model.TipoSena
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LectorContratoTest {

    /** Mismo contenido que docs/modelo.ejemplo.json, con métricas y umbral para cubrir más campos. */
    private val ejemplo = """
        {
          "formato": 1,
          "nombre": "reconocedor-lsc",
          "version": "0.1.0",
          "lengua": "lsc",
          "caracteristicas": { "version": 1, "version_subconjuntos": 1, "por_fotograma": 352 },
          "ventana": { "fotogramas": 32, "duracion_ms": 1000 },
          "salida": {
            "etiquetas": ["sin_sena", "lsc.hola", "lsc.gracias", "lsc.como"],
            "etiqueta_sin_sena": "sin_sena",
            "temperatura": 1.25,
            "umbral_energia": -2.5
          },
          "vocabulario": [
            { "id": "lsc.hola", "glosa": "HOLA", "significado": "hola", "tipo": "palabra" },
            { "id": "lsc.gracias", "glosa": "GRACIAS", "significado": "gracias", "tipo": "palabra" },
            { "id": "lsc.como", "glosa": "CÓMO", "significado": "cómo", "tipo": "palabra", "interrogativa": true }
          ],
          "datos_entrenamiento": ["LSC70 (CC BY 4.0)"],
          "metricas": { "macro_f1": 0.81, "exactitud": 0.86 }
        }
    """.trimIndent()

    private fun errores(texto: String) = (LectorContrato.leer(texto) as? ResultadoContrato.Invalido)?.errores ?: emptyList()

    @Test
    fun `lee un modelo json completo`() {
        val r = LectorContrato.leer(ejemplo)
        assertTrue(r is ResultadoContrato.Valido)
        val c = (r as ResultadoContrato.Valido).contrato

        assertEquals(32, c.fotogramas)
        assertEquals(1_000L, c.duracionVentanaMs)
        assertEquals("sin_sena", c.etiquetaSinSena)
        assertEquals(1.25f, c.temperatura, 1e-6f)
        assertEquals(-2.5f, c.umbralEnergia!!, 1e-6f)
        assertEquals(3, c.entradas.size)
        assertTrue(c.entradas.single { it.id == "lsc.como" }.interrogativa)
        assertEquals(TipoSena.PALABRA, c.entradas[0].tipo)
        assertEquals(0.81f, c.metricas.getValue("macro_f1"), 1e-6f)
        assertTrue(c.usaRostro)
    }

    @Test
    fun `usa_rostro se lee y por defecto es verdadero`() {
        val sinRostro = ejemplo.replace("\"por_fotograma\": 352 }", "\"por_fotograma\": 352, \"usa_rostro\": false }")
        assertTrue("usa_rostro" in sinRostro)
        assertEquals(false, (LectorContrato.leer(sinRostro) as ResultadoContrato.Valido).contrato.usaRostro)

        val malo = ejemplo.replace("\"por_fotograma\": 352 }", "\"por_fotograma\": 352, \"usa_rostro\": \"no\" }")
        assertTrue(errores(malo).single().contains("caracteristicas.usa_rostro"))
    }

    @Test
    fun `los campos opcionales pueden faltar`() {
        val sinOpcionales = ejemplo
            .replace("\"umbral_energia\": -2.5", "\"umbral_energia\": null")
            .replace(",\n  \"metricas\": { \"macro_f1\": 0.81, \"exactitud\": 0.86 }", "")
        assertTrue("metricas" !in sinOpcionales)
        val c = (LectorContrato.leer(sinOpcionales) as ResultadoContrato.Valido).contrato
        assertNull(c.umbralEnergia)
        assertTrue(c.metricas.isEmpty())
    }

    @Test
    fun `informa la ruta del campo que falta o esta mal`() {
        assertTrue(errores(ejemplo.replace("\"fotogramas\": 32", "\"fotogramas\": \"32\"")).single().contains("ventana.fotogramas"))
        assertTrue(errores(ejemplo.replace("\"tipo\": \"palabra\" },", "\"tipo\": \"verbo\" },")).single().contains("vocabulario[0].tipo"))
        assertTrue(errores(ejemplo.replace("\"nombre\": \"reconocedor-lsc\",", "")).single().contains("nombre"))
    }

    @Test
    fun `rechaza otro formato y json roto`() {
        assertTrue(errores(ejemplo.replace("\"formato\": 1", "\"formato\": 2")).single().contains("formato"))
        assertTrue(errores("{ roto").single().contains("no es JSON válido"))
        assertTrue(errores("[]").single().contains("objeto"))
    }

    @Test
    fun `aplica la validacion de compatibilidad`() {
        val incompatible = ejemplo.replace("\"por_fotograma\": 352", "\"por_fotograma\": 100")
        assertTrue(errores(incompatible).any { "100" in it })
    }

    @Test
    fun `un id de vocabulario mal formado se informa sin fallar`() {
        assertTrue(errores(ejemplo.replace("\"id\": \"lsc.hola\"", "\"id\": \"HOLA\"")).isNotEmpty())
    }
}
