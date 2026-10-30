package com.lhansqw.senaslsc.data.history

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Guarda los bytes en un archivo de forma **atómica**: se escribe un temporal y se renombra sobre
 * el definitivo. Si la app se cierra a mitad de una escritura, queda la versión anterior completa,
 * nunca un archivo a medias.
 */
class AlmacenArchivo(private val archivo: File) : Almacen {

    override fun leer(): ByteArray? = if (archivo.isFile) archivo.readBytes() else null

    override fun escribir(datos: ByteArray) {
        archivo.parentFile?.mkdirs()
        val temporal = File(archivo.parentFile, archivo.name + ".tmp")
        temporal.writeBytes(datos)
        try {
            Files.move(temporal.toPath(), archivo.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(temporal.toPath(), archivo.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    override fun borrar() {
        archivo.delete()
        File(archivo.parentFile, archivo.name + ".tmp").delete()
    }
}
