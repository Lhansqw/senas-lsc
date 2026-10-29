package com.lhansqw.senaslsc.core.descargas

import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Copia un flujo (por ejemplo, un archivo elegido por el usuario) a [destino] con progreso y
 * cancelación. Escribe primero en un temporal y lo renombra al terminar: si se cancela o falla,
 * no queda un archivo a medias con el nombre definitivo.
 */
object CopiaArchivo {

    fun copiar(
        entrada: InputStream,
        destino: File,
        total: Long?,
        alProgreso: (Progreso) -> Unit = {},
        cancelado: () -> Boolean = { false },
        reloj: () -> Long = System::currentTimeMillis,
        intervaloProgresoMs: Long = 250,
    ): ResultadoTransferencia {
        destino.parentFile?.mkdirs()
        val temporal = File(destino.parentFile, destino.name + ".copiando")
        val medidor = MedidorProgreso(total?.takeIf { it > 0 })
        try {
            var bytes = 0L
            var ultimoAviso = Long.MIN_VALUE
            entrada.use { e ->
                temporal.outputStream().use { s ->
                    val bloque = ByteArray(64 * 1024)
                    while (true) {
                        if (cancelado()) {
                            s.close()
                            temporal.delete()
                            return ResultadoTransferencia.Cancelada(bytes)
                        }
                        val leidos = e.read(bloque)
                        if (leidos < 0) break
                        s.write(bloque, 0, leidos)
                        bytes += leidos
                        val ahora = reloj()
                        if (ahora - ultimoAviso >= intervaloProgresoMs) {
                            ultimoAviso = ahora
                            alProgreso(medidor.registrar(ahora, bytes))
                        }
                    }
                }
            }
            alProgreso(medidor.registrar(reloj(), bytes))
            if (destino.exists() && !destino.delete()) {
                temporal.delete()
                return ResultadoTransferencia.Fallida("No se pudo reemplazar ${destino.name}", reintentable = false)
            }
            if (!temporal.renameTo(destino)) {
                temporal.delete()
                return ResultadoTransferencia.Fallida("No se pudo guardar ${destino.name}", reintentable = false)
            }
            return ResultadoTransferencia.Completada(destino, bytes)
        } catch (e: IOException) {
            temporal.delete()
            return ResultadoTransferencia.Fallida("No se pudo copiar el archivo: ${e.message}", reintentable = true)
        }
    }
}
