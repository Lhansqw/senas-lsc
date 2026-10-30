package com.lhansqw.senaslsc.contrato

import com.lhansqw.senaslsc.recognizer.LectorContrato
import com.lhansqw.senaslsc.recognizer.ResultadoContrato
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * La app acepta el `modelo.json` que escribe el entrenamiento en Python
 * (`training/pruebas/modelo_generado.json`, generado con `senas/contrato.py`).
 */
class ContratoGeneradoTest {

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
    fun `el contrato escrito por python es valido para la app`() {
        when (val r = LectorContrato.leer(archivo().readText(Charsets.UTF_8))) {
            is ResultadoContrato.Invalido -> fail("La app rechaza el contrato de Python: ${r.errores}")
            is ResultadoContrato.Valido -> {
                val c = r.contrato
                assertEquals("contrato-compartido", c.nombre)
                assertEquals(32, c.fotogramas)
                assertEquals(1000L, c.duracionVentanaMs)
                assertEquals("sin_sena", c.etiquetaSinSena)
                assertEquals(1.25f, c.temperatura, 0f)
                assertEquals(-2.5f, c.umbralEnergia!!, 0f)
                assertEquals(5, c.entradas.size)
                assertTrue(c.entradas.single { it.id == "lsc.como" }.interrogativa)
                assertEquals(0.5f, c.metricas.getValue("exactitud"), 0f)
                assertEquals(false, c.usaRostro)
                assertTrue(c.validar().isEmpty())
            }
        }
    }

    private companion object {
        const val RUTA = "training/pruebas/modelo_generado.json"
    }
}
