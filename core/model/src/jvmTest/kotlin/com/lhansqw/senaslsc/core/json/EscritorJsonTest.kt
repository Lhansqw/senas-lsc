package com.lhansqw.senaslsc.core.json

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class EscritorJsonTest {

    @Test
    fun `escribe valores simples`() {
        assertEquals("null", EscritorJson.escribir(Json.Nulo))
        assertEquals("true", EscritorJson.escribir(true.aJson()))
        assertEquals("42", EscritorJson.escribir(42.aJson()))
        assertEquals("1730000000000", EscritorJson.escribir(1_730_000_000_000L.aJson()))
        assertEquals("0.25", EscritorJson.escribir(0.25.aJson()))
        assertEquals("\"señas\"", EscritorJson.escribir("señas".aJson()))
    }

    @Test
    fun `escapa los caracteres especiales`() {
        assertEquals("\"a\\\"b\\\\c\\nd\\te\\u0001\"", EscritorJson.escribir("a\"b\\c\nd\te\u0001".aJson()))
    }

    @Test
    fun `conserva el orden de los campos`() {
        val obj = json("b" to 1.aJson(), "a" to listOf("x".aJson(), Json.Nulo).aJson())
        assertEquals("{\"b\":1,\"a\":[\"x\",null]}", EscritorJson.escribir(obj))
    }

    @Test
    fun `lo escrito se vuelve a leer igual`() {
        val original = json(
            "texto" to "Hola, ¿cómo estás?\n\"comillas\"".aJson(),
            "numeros" to listOf(0.aJson(), (-3.5).aJson(), 1e-7.aJson()).aJson(),
            "anidado" to json("vacio" to emptyList<Json>().aJson(), "nulo" to Json.Nulo),
            "booleano" to false.aJson(),
        )
        assertEquals(original, LectorJson.leer(EscritorJson.escribir(original)))
    }

    @Test
    fun `rechaza numeros no finitos`() {
        try {
            EscritorJson.escribir(Double.NaN.aJson())
            fail("Se esperaba IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }
}
