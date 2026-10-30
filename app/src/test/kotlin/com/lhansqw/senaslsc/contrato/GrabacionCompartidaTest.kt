package com.lhansqw.senaslsc.contrato

import com.lhansqw.senaslsc.core.grabacion.FormatoGrabacion
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * `training/pruebas/grabacion_ejemplo.json` lo escribió [FormatoGrabacion] y Python lo importa
 * (`test_importar_grabaciones.py`). Si el formato cambia, esta prueba falla: hay que regenerar el
 * archivo y comprobar el importador.
 */
class GrabacionCompartidaTest {

    private fun archivo(): File {
        var carpeta: File? = File(System.getProperty("user.dir")).absoluteFile
        while (carpeta != null) {
            val candidato = File(carpeta, RUTA)
            if (candidato.isFile) return candidato
            carpeta = carpeta.parentFile
        }
        error("No se encontró $RUTA")
    }

    @Test
    fun `el ejemplo compartido se lee y se vuelve a escribir igual`() {
        val texto = archivo().readText(Charsets.UTF_8)
        val muestra = FormatoGrabacion.leer(texto)
        assertEquals("prueba01", muestra.persona)
        assertEquals(37, muestra.fotogramas.size)
        assertEquals(texto, FormatoGrabacion.escribir(muestra))
    }

    private companion object {
        const val RUTA = "training/pruebas/grabacion_ejemplo.json"
    }
}
