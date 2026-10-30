package com.lhansqw.senaslsc.data.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositorioHistorialTest {

    private var ahora = 1_000L
    private var siguiente = 0

    private fun repositorio(almacen: Almacen = AlmacenMemoria(), cifrador: Cifrador = CifradorPrueba(), maximo: Int = 500) =
        RepositorioHistorial(almacen, cifrador, maximo, reloj = { ahora++ }, nuevoId = { "id${siguiente++}" })

    @Test
    fun `agrega y lista de la mas reciente a la mas antigua`() {
        val r = repositorio()
        r.agregar("Primera", listOf("A"), OrigenTexto.REGLAS)
        r.agregar("  Segunda  ", listOf("B"), OrigenTexto.IA_LOCAL)

        assertEquals(listOf("Segunda", "Primera"), r.listar().map { it.texto })
    }

    @Test
    fun `persiste cifrado y se recupera en otra instancia`() {
        val almacen = AlmacenMemoria()
        repositorio(almacen).agregar("Hola, gracias.", listOf("HOLA", "GRACIAS"), OrigenTexto.REGLAS)

        val enDisco = almacen.datos!!.toString(Charsets.UTF_8)
        assertFalse("el texto no debe guardarse en claro", "gracias" in enDisco)

        val otra = repositorio(almacen)
        assertEquals("Hola, gracias.", otra.listar().single().texto)
        assertFalse(otra.recuperadoDeError)
    }

    @Test
    fun `elimina una entrada y vacia todo`() {
        val almacen = AlmacenMemoria()
        val r = repositorio(almacen)
        val a = r.agregar("A", emptyList(), OrigenTexto.REGLAS)
        r.agregar("B", emptyList(), OrigenTexto.REGLAS)

        assertTrue(r.eliminar(a.id))
        assertFalse(r.eliminar("no-existe"))
        assertEquals(listOf("B"), r.listar().map { it.texto })

        r.vaciar()
        assertTrue(r.listar().isEmpty())
        assertNull(almacen.datos)
    }

    @Test
    fun `respeta el maximo eliminando las mas antiguas`() {
        val r = repositorio(maximo = 3)
        (1..5).forEach { r.agregar("Frase $it", emptyList(), OrigenTexto.REGLAS) }
        assertEquals(listOf("Frase 5", "Frase 4", "Frase 3"), r.listar().map { it.texto })
    }

    @Test
    fun `busca sin importar tildes en el texto y en las glosas`() {
        val r = repositorio()
        r.agregar("¿Cómo estás?", listOf("COMO", "ESTAR"), OrigenTexto.IA_LOCAL)
        r.agregar("Buenos días", listOf("BUENOS_DIAS"), OrigenTexto.REGLAS)

        assertEquals(listOf("¿Cómo estás?"), r.buscar("como").map { it.texto })
        assertEquals(listOf("Buenos días"), r.buscar("buenos_dias").map { it.texto })
        assertEquals(2, r.buscar("").size)
    }

    @Test
    fun `si no se puede descifrar empieza vacio y lo indica`() {
        val almacen = AlmacenMemoria().apply { datos = "basura".toByteArray() }
        val r = repositorio(almacen, CifradorRoto())

        assertTrue(r.listar().isEmpty())
        assertTrue(r.recuperadoDeError)

        // La siguiente escritura sustituye los datos ilegibles. CifradorRoto no transforma al cifrar,
        // así que se puede leer con un cifrador de prueba neutro (XOR con 0).
        r.agregar("Nueva", emptyList(), OrigenTexto.REGLAS)
        assertEquals("Nueva", repositorio(almacen, CifradorPrueba(clave = 0)).listar().singleOrNull()?.texto)
    }

    @Test
    fun `no escribe si no hay cambios`() {
        val almacen = AlmacenMemoria()
        val r = repositorio(almacen)
        r.listar()
        r.eliminar("nada")
        assertEquals(0, almacen.escrituras)
    }
}
