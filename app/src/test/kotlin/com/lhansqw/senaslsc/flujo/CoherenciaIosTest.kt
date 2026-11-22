package com.lhansqw.senaslsc.flujo

import com.lhansqw.senaslsc.acerca.Creditos
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * La app de iPhone (ios/) repite en Swift las citas obligatorias de LSC70 y LSC50 que están en Kotlin.
 * Esta prueba falla si se desincronizan.
 */
class CoherenciaIosTest {

    /** Las pruebas de :app se ejecutan con app/ como carpeta de trabajo. */
    private fun swift(ruta: String): String {
        val archivo = File("../ios", ruta)
        assertTrue("No se encuentra ${archivo.absolutePath}", archivo.isFile)
        return archivo.readText()
    }

    @Test
    fun `las citas de LSC70 y LSC50 son las mismas en iPhone`() {
        val acerca = swift("SenasLSC/Pantallas/PantallaAcerca.swift")
        assertTrue("La cita de LSC70 en Swift no coincide con Creditos.CITA_LSC70", "\"${Creditos.CITA_LSC70}\"" in acerca)
        assertTrue("\"${Creditos.datos.enlace}\"" in acerca)
        assertTrue("La cita de LSC50 en Swift no coincide con Creditos.CITA_LSC50", "\"${Creditos.CITA_LSC50}\"" in acerca)
        assertTrue("\"${Creditos.datosLsc50.enlace}\"" in acerca)
    }
}
