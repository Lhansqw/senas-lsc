package com.lhansqw.senaslsc.data.history

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.nio.file.Files
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class CifradoYArchivoTest {

    private fun claveNueva(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val clave = claveNueva()
    private val cifrador = CifradorAesGcm { clave }
    private val mensaje = "Me llamo Nik. ¿Cómo estás?".toByteArray()

    private fun debeFallar(bloque: () -> Unit) {
        try {
            bloque()
            fail("Se esperaba un error de descifrado")
        } catch (_: Exception) {
        }
    }

    @Test
    fun `cifra y descifra con la misma clave`() {
        assertArrayEquals(mensaje, cifrador.descifrar(cifrador.cifrar(mensaje)))
    }

    @Test
    fun `cada cifrado es distinto gracias al iv aleatorio`() {
        val a = cifrador.cifrar(mensaje)
        val b = cifrador.cifrar(mensaje)
        assertFalse(a.contentEquals(b))
        assertFalse("el texto no debe aparecer en claro", String(a, Charsets.ISO_8859_1).contains("Nik"))
    }

    @Test
    fun `detecta datos modificados`() {
        val cifrado = cifrador.cifrar(mensaje)
        cifrado[cifrado.size - 1] = (cifrado.last().toInt() xor 1).toByte()
        debeFallar { cifrador.descifrar(cifrado) }
    }

    @Test
    fun `con otra clave no se puede descifrar`() {
        val cifrado = cifrador.cifrar(mensaje)
        debeFallar { CifradorAesGcm { claveNueva() }.descifrar(cifrado) }
    }

    @Test
    fun `rechaza bloques mal formados`() {
        debeFallar { cifrador.descifrar(byteArrayOf()) }
        debeFallar { cifrador.descifrar(byteArrayOf(2, 12)) }
        debeFallar { cifrador.descifrar(byteArrayOf(1, 3, 0, 0, 0)) }
    }

    @Test
    fun `el archivo se escribe y se reemplaza de forma atomica`() {
        val carpeta = Files.createTempDirectory("historial").toFile()
        val archivo = java.io.File(carpeta, "historial.bin")
        val almacen = AlmacenArchivo(archivo)

        assertNull(almacen.leer())
        almacen.escribir(byteArrayOf(1, 2, 3))
        almacen.escribir(byteArrayOf(4, 5))
        assertArrayEquals(byteArrayOf(4, 5), almacen.leer())
        assertFalse(java.io.File(carpeta, "historial.bin.tmp").exists())

        almacen.borrar()
        assertNull(almacen.leer())
    }

    @Test
    fun `repositorio real con aes y archivo sobrevive a una nueva instancia`() {
        val archivo = java.io.File(Files.createTempDirectory("historial").toFile(), "h.bin")
        RepositorioHistorial(AlmacenArchivo(archivo), cifrador).agregar("Hola, gracias.", listOf("HOLA", "GRACIAS"), OrigenTexto.REGLAS)

        val leido = RepositorioHistorial(AlmacenArchivo(archivo), cifrador).listar()
        assertEquals("Hola, gracias.", leido.single().texto)
        assertTrue(archivo.length() > 0)

        // Tras «reinstalar» (otra clave), empieza vacío y avisa.
        val otraClave = RepositorioHistorial(AlmacenArchivo(archivo), CifradorAesGcm { claveNueva() })
        assertTrue(otraClave.listar().isEmpty())
        assertTrue(otraClave.recuperadoDeError)
    }
}
