package com.lhansqw.senaslsc.core.descargas

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI

/** Resultado de una descarga o de una copia de archivo. */
sealed interface ResultadoTransferencia {
    data class Completada(val archivo: File, val bytes: Long) : ResultadoTransferencia

    /** El usuario canceló. En una descarga, el archivo parcial se conserva para continuar luego. */
    data class Cancelada(val bytes: Long) : ResultadoTransferencia
    data class Fallida(val mensaje: String, val reintentable: Boolean) : ResultadoTransferencia
}

/**
 * Descarga un archivo grande por HTTP(S) con **reanudación**: los bytes se escriben en
 * `<destino>.parcial` y, si la descarga se interrumpe, la siguiente continúa desde ahí con la
 * cabecera `Range`. Al terminar, el parcial se renombra a [destino].
 *
 * Es bloqueante y solo usa Java estándar, así que se prueba en la JVM; en Android se llama desde
 * un hilo de entrada/salida. La cancelación se comprueba entre bloques con [cancelado].
 */
class DescargadorArchivo(
    private val reloj: () -> Long = System::currentTimeMillis,
    private val tamanoBloque: Int = 64 * 1024,
    private val tiempoEsperaMs: Int = 30_000,
    /** Cada cuánto se informa del progreso, como mucho. */
    private val intervaloProgresoMs: Long = 250,
) {

    fun parcialDe(destino: File): File = File(destino.parentFile, destino.name + SUFIJO_PARCIAL)

    fun descargar(
        url: String,
        destino: File,
        alProgreso: (Progreso) -> Unit = {},
        cancelado: () -> Boolean = { false },
    ): ResultadoTransferencia {
        destino.parentFile?.mkdirs()
        val parcial = parcialDe(destino)
        var descargados = if (parcial.isFile) parcial.length() else 0L

        val conexion = try {
            (URI(url).toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = tiempoEsperaMs
                readTimeout = tiempoEsperaMs
                instanceFollowRedirects = true
                setRequestProperty("Accept-Encoding", "identity") // sin compresión: los bytes cuadran con Range
                Reanudacion.cabeceraRango(descargados)?.let { setRequestProperty("Range", it) }
            }
        } catch (e: Exception) {
            return ResultadoTransferencia.Fallida("Dirección de descarga no válida: ${e.message}", reintentable = false)
        }

        try {
            val codigo = conexion.responseCode
            val total: Long?
            val anadir: Boolean
            when (val d = Reanudacion.decidir(descargados, codigo, conexion.getHeaderField("Content-Range"), conexion.contentLengthLong)) {
                is Reanudacion.Decision.Fallo -> return ResultadoTransferencia.Fallida(d.mensaje, d.reintentable)
                is Reanudacion.Decision.YaCompleto -> return finalizar(parcial, destino, d.total)
                is Reanudacion.Decision.EmpezarDeCero -> {
                    if (codigo != 200) {
                        // 416 con un parcial que no cuadra: se descarta y la próxima vez empieza de cero.
                        parcial.delete()
                        return ResultadoTransferencia.Fallida("El archivo parcial no era válido; vuelve a intentarlo.", reintentable = true)
                    }
                    descargados = 0
                    total = d.total
                    anadir = false
                }
                is Reanudacion.Decision.Continuar -> {
                    total = d.total ?: conexion.contentLengthLong.takeIf { it > 0 }?.let { it + descargados }
                    anadir = true
                }
            }

            val medidor = MedidorProgreso(total, descargados)
            val resultado = conexion.inputStream.use { entrada ->
                FileOutputStream(parcial, anadir).use { salida ->
                    copiar(entrada, salida, medidor, alProgreso, cancelado)
                }
            }
            if (resultado != null) return resultado

            if (total != null && medidor.bytes != total) {
                return ResultadoTransferencia.Fallida(
                    "La descarga se cortó (${formatearTamano(medidor.bytes)} de ${formatearTamano(total)}). Vuelve a intentarlo para continuar.",
                    reintentable = true,
                )
            }
            return finalizar(parcial, destino, medidor.bytes)
        } catch (e: IOException) {
            return ResultadoTransferencia.Fallida(
                "Error de red: ${e.message ?: e.javaClass.simpleName}. Lo descargado se conserva; vuelve a intentarlo para continuar.",
                reintentable = true,
            )
        } finally {
            conexion.disconnect()
        }
    }

    /** Copia bloques actualizando el progreso. Devuelve [ResultadoTransferencia.Cancelada] o `null` si terminó. */
    private fun copiar(
        entrada: InputStream,
        salida: FileOutputStream,
        medidor: MedidorProgreso,
        alProgreso: (Progreso) -> Unit,
        cancelado: () -> Boolean,
    ): ResultadoTransferencia? {
        val bloque = ByteArray(tamanoBloque)
        var ultimoAviso = Long.MIN_VALUE
        var bytes = medidor.bytes
        alProgreso(medidor.registrar(reloj(), bytes))
        while (true) {
            if (cancelado()) {
                salida.flush()
                return ResultadoTransferencia.Cancelada(bytes)
            }
            val leidos = entrada.read(bloque)
            if (leidos < 0) break
            salida.write(bloque, 0, leidos)
            bytes += leidos
            val ahora = reloj()
            if (ahora - ultimoAviso >= intervaloProgresoMs) {
                ultimoAviso = ahora
                alProgreso(medidor.registrar(ahora, bytes))
            }
        }
        alProgreso(medidor.registrar(reloj(), bytes))
        return null
    }

    private fun finalizar(parcial: File, destino: File, bytes: Long): ResultadoTransferencia {
        if (destino.exists() && !destino.delete()) {
            return ResultadoTransferencia.Fallida("No se pudo reemplazar ${destino.name}", reintentable = false)
        }
        if (!parcial.renameTo(destino)) {
            return ResultadoTransferencia.Fallida("No se pudo guardar ${destino.name}", reintentable = false)
        }
        return ResultadoTransferencia.Completada(destino, bytes)
    }

    companion object {
        const val SUFIJO_PARCIAL = ".parcial"
    }
}
