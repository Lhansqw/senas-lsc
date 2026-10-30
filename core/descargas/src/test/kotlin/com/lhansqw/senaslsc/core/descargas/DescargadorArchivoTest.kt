package com.lhansqw.senaslsc.core.descargas

import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import kotlin.random.Random

/**
 * Prueba la descarga contra un servidor HTTP real en el propio ordenador, que admite (o no)
 * peticiones con `Range`, como los servidores de modelos.
 */
class DescargadorArchivoTest {

    private val datos = Random(42).nextBytes(300_000)

    /** Arranca un servidor que sirve [datos] y ejecuta [bloque] con su URL. */
    private fun conServidor(admiteRango: Boolean = true, codigoFijo: Int? = null, bloque: (String) -> Unit) {
        val servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/modelo.bin") { ex ->
            val rango = ex.requestHeaders.getFirst("Range")
            when {
                codigoFijo != null -> ex.sendResponseHeaders(codigoFijo, -1)
                admiteRango && rango != null -> {
                    val inicio = rango.removePrefix("bytes=").removeSuffix("-").toInt()
                    if (inicio >= datos.size) {
                        ex.responseHeaders.add("Content-Range", "bytes */${datos.size}")
                        ex.sendResponseHeaders(416, -1)
                    } else {
                        ex.responseHeaders.add("Content-Range", "bytes $inicio-${datos.size - 1}/${datos.size}")
                        ex.sendResponseHeaders(206, (datos.size - inicio).toLong())
                        ex.responseBody.use { it.write(datos, inicio, datos.size - inicio) }
                    }
                }
                else -> {
                    ex.sendResponseHeaders(200, datos.size.toLong())
                    ex.responseBody.use { it.write(datos) }
                }
            }
            ex.close()
        }
        servidor.start()
        try {
            bloque("http://127.0.0.1:${servidor.address.port}/modelo.bin")
        } finally {
            servidor.stop(0)
        }
    }

    private fun carpeta(): File = Files.createTempDirectory("descarga").toFile().apply { deleteOnExit() }

    @Test
    fun `descarga completa y renombra el parcial`() = conServidor { url ->
        val destino = File(carpeta(), "modelo.bin")
        val progresos = mutableListOf<Progreso>()
        val r = DescargadorArchivo(tamanoBloque = 8_192, intervaloProgresoMs = 0).descargar(url, destino, { progresos += it })

        assertTrue(r is ResultadoTransferencia.Completada)
        assertArrayEquals(datos, destino.readBytes())
        assertFalse(File(destino.path + DescargadorArchivo.SUFIJO_PARCIAL).exists())
        assertEquals(datos.size.toLong(), progresos.last().bytes)
        assertEquals(1f, progresos.last().fraccion!!, 0f)
    }

    @Test
    fun `cancelar conserva el parcial y la siguiente descarga continua`() = conServidor { url ->
        val destino = File(carpeta(), "modelo.bin")
        val d = DescargadorArchivo(tamanoBloque = 10_000, intervaloProgresoMs = 0)

        var bloques = 0
        val cancelada = d.descargar(url, destino, cancelado = { bloques++ >= 10 })
        assertTrue(cancelada is ResultadoTransferencia.Cancelada)
        // read() puede devolver menos que un bloque: 10 lecturas son como mucho 100 000 bytes.
        val parcial = d.parcialDe(destino).length()
        assertTrue("parcial: $parcial", parcial in 1..100_000L)
        assertArrayEquals(datos.copyOf(parcial.toInt()), d.parcialDe(destino).readBytes())

        var primerProgreso: Progreso? = null
        val r = d.descargar(url, destino, { if (primerProgreso == null) primerProgreso = it })
        assertTrue(r is ResultadoTransferencia.Completada)
        assertEquals(parcial, primerProgreso!!.bytes)              // continuó, no empezó de cero
        assertArrayEquals(datos, destino.readBytes())
    }

    @Test
    fun `si el servidor ignora el rango empieza de cero sin corromper el archivo`() = conServidor(admiteRango = false) { url ->
        val destino = File(carpeta(), "modelo.bin")
        val d = DescargadorArchivo(tamanoBloque = 10_000, intervaloProgresoMs = 0)
        var bloques = 0
        d.descargar(url, destino, cancelado = { bloques++ >= 5 })

        val r = d.descargar(url, destino)
        assertTrue(r is ResultadoTransferencia.Completada)
        assertArrayEquals(datos, destino.readBytes())
    }

    @Test
    fun `un parcial ya completo se da por terminado`() = conServidor { url ->
        val destino = File(carpeta(), "modelo.bin")
        val d = DescargadorArchivo()
        d.parcialDe(destino).writeBytes(datos)

        val r = d.descargar(url, destino)
        assertTrue(r is ResultadoTransferencia.Completada)
        assertArrayEquals(datos, destino.readBytes())
    }

    @Test
    fun `los errores del servidor se informan con su mensaje`() {
        conServidor(codigoFijo = 404) { url ->
            val r = DescargadorArchivo().descargar(url, File(carpeta(), "m.bin")) as ResultadoTransferencia.Fallida
            assertTrue(r.mensaje.contains("404"))
            assertFalse(r.reintentable)
        }
        conServidor(codigoFijo = 503) { url ->
            val r = DescargadorArchivo().descargar(url, File(carpeta(), "m.bin")) as ResultadoTransferencia.Fallida
            assertTrue(r.reintentable)
        }
    }

    @Test
    fun `sin servidor el fallo es reintentable y una url rota no`() {
        val sinServidor = DescargadorArchivo(tiempoEsperaMs = 2_000).descargar("http://127.0.0.1:1/x", File(carpeta(), "m.bin"))
        assertTrue((sinServidor as ResultadoTransferencia.Fallida).reintentable)

        val rota = DescargadorArchivo().descargar("no es una url", File(carpeta(), "m.bin"))
        assertFalse((rota as ResultadoTransferencia.Fallida).reintentable)
    }

    @Test
    fun `la copia de un archivo importado escribe todo o nada`() {
        val destino = File(carpeta(), "importado.bin")
        val r = CopiaArchivo.copiar(datos.inputStream(), destino, datos.size.toLong(), intervaloProgresoMs = 0)
        assertTrue(r is ResultadoTransferencia.Completada)
        assertArrayEquals(datos, destino.readBytes())

        val otro = File(carpeta(), "cancelado.bin")
        var lecturas = 0
        val cancelada = CopiaArchivo.copiar(datos.inputStream(), otro, datos.size.toLong(), cancelado = { lecturas++ >= 2 })
        assertTrue(cancelada is ResultadoTransferencia.Cancelada)
        assertFalse(otro.exists())
        assertFalse(File(otro.parentFile, otro.name + ".copiando").exists())
    }
}
