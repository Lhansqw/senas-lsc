package com.lhansqw.senaslsc.interpreter.litertlm

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import com.lhansqw.senaslsc.core.descargas.CopiaArchivo
import com.lhansqw.senaslsc.core.descargas.DescargadorArchivo
import com.lhansqw.senaslsc.core.descargas.EspacioNecesario
import com.lhansqw.senaslsc.core.descargas.Progreso
import com.lhansqw.senaslsc.core.descargas.ResultadoTransferencia
import com.lhansqw.senaslsc.core.descargas.formatearTamano
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/** Situación del archivo del modelo de lenguaje en el teléfono. */
sealed interface EstadoModeloLenguaje {
    data object NoInstalado : EstadoModeloLenguaje

    /** Hay una descarga interrumpida que se puede continuar. */
    data class Parcial(val bytes: Long, val total: Long) : EstadoModeloLenguaje
    data class Descargando(val progreso: Progreso) : EstadoModeloLenguaje
    data class Importando(val progreso: Progreso) : EstadoModeloLenguaje
    data class Instalado(val bytes: Long) : EstadoModeloLenguaje
    data class EspacioInsuficiente(val requerido: Long, val libre: Long) : EstadoModeloLenguaje
    data class Error(val mensaje: String, val reintentable: Boolean) : EstadoModeloLenguaje
}

/**
 * Instala, comprueba y elimina el archivo del modelo de lenguaje.
 *
 * - El modelo se guarda en el almacenamiento privado de la app (`files/modelos/`), que está
 *   excluido de las copias de seguridad.
 * - La descarga es reanudable: si se corta o se cancela, la siguiente continúa donde quedó.
 * - Antes de descargar o importar se comprueba que hay espacio libre suficiente.
 *
 * Debe vivir lo mismo que la app (no una pantalla), para que la descarga no se interrumpa al
 * cambiar de pantalla.
 */
class GestorModeloLenguaje(
    context: Context,
    private val ambito: CoroutineScope,
    val descriptor: DescriptorModelo = ModelosLenguaje.POR_DEFECTO,
) {
    private val contexto = context.applicationContext
    private val carpeta = File(contexto.filesDir, CARPETA)
    val archivo: File = File(carpeta, descriptor.archivo)

    private val descargador = DescargadorArchivo()
    private var trabajo: Job? = null

    @Volatile
    private var cancelacionPedida = false

    private val _estado = MutableStateFlow(estadoEnDisco())
    val estado: StateFlow<EstadoModeloLenguaje> = _estado.asStateFlow()

    /** Ruta del modelo si está instalado y es plausible; si no, `null`. */
    val rutaInstalado: String?
        get() = archivo.takeIf { it.isFile && descriptor.esArchivoPlausible(it.length()) }?.absolutePath

    val ocupado: Boolean get() = trabajo?.isActive == true

    fun refrescar() {
        if (!ocupado) _estado.value = estadoEnDisco()
    }

    /** Descarga (o continúa descargando) el modelo. Devuelve `false` si ya hay una operación en curso. */
    fun descargar(): Boolean {
        if (ocupado) return false
        val parcial = descargador.parcialDe(archivo)
        val yaDescargado = if (parcial.isFile) parcial.length() else 0L
        if (!hayEspacio(descriptor.tamanoAproximado, yaDescargado)) return false

        cancelacionPedida = false
        trabajo = ambito.launch(Dispatchers.IO) {
            _estado.value = EstadoModeloLenguaje.Descargando(Progreso(yaDescargado, descriptor.tamanoAproximado, 0.0))
            val resultado = descargador.descargar(
                url = descriptor.url,
                destino = archivo,
                alProgreso = { _estado.value = EstadoModeloLenguaje.Descargando(it) },
                cancelado = { cancelacionPedida },
            )
            _estado.value = tras(resultado)
        }
        return true
    }

    /** Copia el modelo desde un archivo elegido por el usuario (por ejemplo, descargado en el PC). */
    fun importar(origen: Uri): Boolean {
        if (ocupado) return false
        val (nombre, tamano) = datosDe(origen)
        if (nombre != null && !nombre.endsWith(DescriptorModelo.EXTENSION, ignoreCase = true)) {
            _estado.value = EstadoModeloLenguaje.Error(
                "El archivo «$nombre» no es un modelo LiteRT-LM (debe terminar en ${DescriptorModelo.EXTENSION}).",
                reintentable = false,
            )
            return false
        }
        if (tamano != null && !descriptor.esArchivoPlausible(tamano)) {
            _estado.value = EstadoModeloLenguaje.Error(
                "El archivo ocupa ${formatearTamano(tamano)}; el modelo ocupa unos ${formatearTamano(descriptor.tamanoAproximado)}.",
                reintentable = false,
            )
            return false
        }
        if (!hayEspacio(tamano ?: descriptor.tamanoAproximado, 0)) return false

        cancelacionPedida = false
        trabajo = ambito.launch(Dispatchers.IO) {
            _estado.value = EstadoModeloLenguaje.Importando(Progreso(0, tamano, 0.0))
            val resultado = try {
                val entrada = contexto.contentResolver.openInputStream(origen)
                    ?: throw IOException("No se pudo abrir el archivo")
                CopiaArchivo.copiar(
                    entrada = entrada,
                    destino = archivo,
                    total = tamano,
                    alProgreso = { _estado.value = EstadoModeloLenguaje.Importando(it) },
                    cancelado = { cancelacionPedida },
                )
            } catch (e: IOException) {
                ResultadoTransferencia.Fallida("No se pudo leer el archivo: ${e.message}", reintentable = true)
            } catch (e: SecurityException) {
                ResultadoTransferencia.Fallida("La app no tiene permiso para leer ese archivo.", reintentable = false)
            }
            _estado.value = tras(resultado)
        }
        return true
    }

    /** Pide detener la descarga o la copia en curso (la descarga parcial se conserva). */
    fun cancelar() {
        cancelacionPedida = true
    }

    /** Borra el modelo y cualquier descarga parcial. */
    fun eliminar() {
        if (ocupado) return
        archivo.delete()
        descargador.parcialDe(archivo).delete()
        _estado.value = EstadoModeloLenguaje.NoInstalado
    }

    private fun tras(resultado: ResultadoTransferencia): EstadoModeloLenguaje = when (resultado) {
        is ResultadoTransferencia.Completada ->
            if (descriptor.esArchivoPlausible(resultado.bytes)) {
                EstadoModeloLenguaje.Instalado(resultado.bytes)
            } else {
                resultado.archivo.delete()
                EstadoModeloLenguaje.Error(
                    "El archivo recibido (${formatearTamano(resultado.bytes)}) es demasiado pequeño para ser el modelo.",
                    reintentable = true,
                )
            }
        is ResultadoTransferencia.Cancelada -> estadoEnDisco()
        is ResultadoTransferencia.Fallida -> EstadoModeloLenguaje.Error(resultado.mensaje, resultado.reintentable)
    }

    private fun estadoEnDisco(): EstadoModeloLenguaje {
        if (archivo.isFile && descriptor.esArchivoPlausible(archivo.length())) {
            return EstadoModeloLenguaje.Instalado(archivo.length())
        }
        val parcial = descargador.parcialDe(archivo)
        if (parcial.isFile && parcial.length() > 0) {
            return EstadoModeloLenguaje.Parcial(parcial.length(), descriptor.tamanoAproximado)
        }
        return EstadoModeloLenguaje.NoInstalado
    }

    private fun hayEspacio(total: Long, yaDescargado: Long): Boolean {
        carpeta.mkdirs()
        val libre = StatFs(carpeta.absolutePath).availableBytes
        if (EspacioNecesario.suficiente(libre, total, yaDescargado)) return true
        _estado.value = EstadoModeloLenguaje.EspacioInsuficiente(EspacioNecesario.requerido(total, yaDescargado), libre)
        return false
    }

    /** Nombre y tamaño que informa el proveedor del archivo, si los informa. */
    private fun datosDe(uri: Uri): Pair<String?, Long?> = try {
        contexto.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return null to null
                val nombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { cursor.getString(it) }
                val tamano = cursor.getColumnIndex(OpenableColumns.SIZE)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let { cursor.getLong(it) }
                nombre to tamano
            } ?: (null to null)
    } catch (e: SecurityException) {
        null to null
    }

    private companion object {
        const val CARPETA = "modelos"
    }
}
