package com.lhansqw.senaslsc.flujo

import com.lhansqw.senaslsc.acerca.Creditos
import com.lhansqw.senaslsc.reconocimiento.EstadoReloj
import com.lhansqw.senaslsc.reconocimiento.ResumenReloj
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * La app de iPhone y la del Apple Watch (ios/) repiten en Swift algunos textos de Kotlin: las citas
 * obligatorias de LSC70 y LSC50 y las claves de los mensajes del reloj. Esta prueba falla si se desincronizan.
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

    @Test
    fun `el reloj usa las mismas claves, estados y ordenes`() {
        val mensaje = swift("Comun/MensajeReloj.swift")
        val valores = listOf(
            ResumenReloj.Claves.ESTADO,
            ResumenReloj.Claves.FRASE,
            ResumenReloj.Claves.ULTIMA_SENA,
            ResumenReloj.Claves.NUMERO_SENAS,
            ResumenReloj.Claves.INCIERTA,
            ResumenReloj.Claves.PAUSADO,
            ResumenReloj.Ordenes.CLAVE,
        ) + EstadoReloj.entries.map { it.name }
        valores.forEach { assertTrue("Falta \"$it\" en MensajeReloj.swift", "\"$it\"" in mensaje) }

        // Las órdenes son los nombres de los casos del enum de Swift.
        val ordenes = listOf(
            ResumenReloj.Ordenes.INICIAR,
            ResumenReloj.Ordenes.PAUSAR,
            ResumenReloj.Ordenes.BORRAR,
            ResumenReloj.Ordenes.INTERPRETAR,
        )
        val casos = Regex("""case\s+([a-z, ]+)\n""").findAll(mensaje.substringAfter("enum Orden")).first().groupValues[1]
        ordenes.forEach { assertTrue("Falta la orden $it en MensajeReloj.swift", it in casos.split(',').map(String::trim)) }
    }
}
