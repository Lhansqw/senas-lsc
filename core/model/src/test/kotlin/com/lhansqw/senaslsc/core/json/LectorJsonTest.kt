package com.lhansqw.senaslsc.core.json

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LectorJsonTest {

    private fun invalido(texto: String) {
        try {
            LectorJson.leer(texto)
            fail("Se esperaba JsonInvalido para: $texto")
        } catch (_: JsonInvalido) {
        }
    }

    @Test
    fun `lee valores simples`() {
        assertEquals(Json.Numero(42.0), LectorJson.leer("42"))
        assertEquals(Json.Numero(-0.5), LectorJson.leer("-0.5"))
        assertEquals(Json.Numero(1.5e-3), LectorJson.leer("1.5e-3"))
        assertEquals(Json.Booleano(true), LectorJson.leer("true"))
        assertEquals(Json.Nulo, LectorJson.leer(" null "))
        assertEquals(Json.Texto("señas"), LectorJson.leer("\"señas\""))
    }

    @Test
    fun `lee objetos y listas anidados conservando el orden`() {
        val json = LectorJson.leer("""{"b": [1, {"c": null}], "a": "x"}""") as Json.Objeto
        assertEquals(listOf("b", "a"), json.campos.keys.toList())
        val lista = json["b"] as Json.Lista
        assertEquals(Json.Numero(1.0), lista.elementos[0])
        assertEquals(Json.Nulo, (lista.elementos[1] as Json.Objeto)["c"])
    }

    @Test
    fun `interpreta los escapes`() {
        val t = LectorJson.leer(""""a\"b\\c\/d\n\t\u00f1"""") as Json.Texto
        assertEquals("a\"b\\c/d\n\tñ", t.valor)
    }

    @Test
    fun `rechaza documentos invalidos`() {
        listOf(
            "", "{", "[1,]", "{\"a\":}", "{a: 1}", "01", "1.", "-", "1e", "tru", "\"sin cerrar",
            "{\"a\": 1} extra", "\"\\x\"", "[1 2]", "\"a\nb\"",
        ).forEach(::invalido)
    }

    @Test
    fun `el error indica la posicion`() {
        try {
            LectorJson.leer("{\"a\": 1,, }")
            fail()
        } catch (e: JsonInvalido) {
            assertEquals(8, e.posicion)
        }
    }

    @Test
    fun `limita la profundidad para evitar desbordamientos`() {
        invalido("[".repeat(100) + "]".repeat(100))
    }

    @Test
    fun `extrae el objeto aunque haya texto alrededor`() {
        val respuesta = "Claro, aquí está:\n```json\n{\"propuesta\": \"Hola {amigo}\", \"n\": 1}\n```\nEspero que sirva."
        val obj = LectorJson.extraerPrimerObjeto(respuesta)
        assertEquals("Hola {amigo}", obj?.texto("propuesta"))
    }

    @Test
    fun `salta objetos rotos y devuelve null si no hay ninguno`() {
        assertEquals(2.0, LectorJson.extraerPrimerObjeto("{roto} {\"n\": 2}")?.numero("n"))
        assertNull(LectorJson.extraerPrimerObjeto("sin json"))
        assertNull(LectorJson.extraerPrimerObjeto("{\"abierto\": 1"))
    }

    @Test
    fun `los accesos validan el tipo y dan la ruta del campo`() {
        val obj = LectorJson.leer("""{"n": 2.5, "e": 3, "t": "x", "l": ["a", 1], "b": false}""") as Json.Objeto
        assertEquals(3L, obj.entero("e"))
        assertEquals("x", obj.texto("t"))
        assertEquals(false, obj.booleano("b"))
        assertNull(obj.textoOpcional("falta"))

        try {
            obj.entero("n")
            fail()
        } catch (e: CampoInvalido) {
            assertEquals("n", e.ruta)
        }
        try {
            obj.listaTextos("l", "modelo.l")
            fail()
        } catch (e: CampoInvalido) {
            assertEquals("modelo.l[1]", e.ruta)
            assertTrue(e.message!!.contains("texto"))
        }
    }
}
