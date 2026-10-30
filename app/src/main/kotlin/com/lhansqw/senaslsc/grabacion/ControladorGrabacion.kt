package com.lhansqw.senaslsc.grabacion

import android.content.Context
import android.util.Log
import com.lhansqw.senaslsc.core.grabacion.EstadoGrabacion
import com.lhansqw.senaslsc.core.grabacion.FormatoGrabacion
import com.lhansqw.senaslsc.core.grabacion.GrabadorMuestra
import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.recognizer.ResultadoContrato
import com.lhansqw.senaslsc.recognizer.litert.CargadorReconocedor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/** Una etiqueta que se puede grabar: una seña del modelo instalado o «sin seña». */
data class EtiquetaGrabacion(val id: String, val glosa: String)

/** Lo que muestra el panel de grabación. */
data class EstadoGrabador(
    val grabacion: EstadoGrabacion = EstadoGrabacion.Inactivo,
    val etiquetas: List<EtiquetaGrabacion> = emptyList(),
    /** Muestras guardadas de cada etiqueta para el alias actual. */
    val guardadas: Map<String, Int> = emptyMap(),
    val mensaje: String? = null,
)

/**
 * Herramienta de grabación (fase 6): guarda los puntos de MediaPipe de una seña como JSON en
 * `Android/data/com.lhansqw.senaslsc/files/grabaciones/`. Nunca guarda imágenes.
 *
 * [recibir] se llama desde el hilo de análisis con cada fotograma; las órdenes llegan del hilo
 * principal. Todo el acceso al grabador pasa por el mismo cerrojo, y la escritura de archivos se
 * hace en un hilo propio.
 */
class ControladorGrabacion(context: Context) : AutoCloseable {

    private val contextoApp = context.applicationContext
    private val bloqueo = Any()
    private val grabador = GrabadorMuestra()
    private val disco = Executors.newSingleThreadExecutor { Thread(it, "senas-grabacion") }

    /** Carpeta de las grabaciones; se borra al desinstalar la app. */
    val carpeta: File = File(contextoApp.getExternalFilesDir(null) ?: contextoApp.filesDir, "grabaciones")

    private val _estado = MutableStateFlow(EstadoGrabador())
    val estado: StateFlow<EstadoGrabador> = _estado.asStateFlow()

    @Volatile
    private var alias = ""

    init {
        disco.execute {
            val etiquetas = when (val r = CargadorReconocedor.leerContrato(contextoApp)) {
                is ResultadoContrato.Valido -> r.contrato.entradas.map { EtiquetaGrabacion(it.id, it.glosa) }
                else -> emptyList()
            }
            _estado.update { it.copy(etiquetas = listOf(EtiquetaGrabacion(FormatoGrabacion.SIN_SENA, "SIN SEÑA")) + etiquetas) }
        }
    }

    /** Cambia el alias de la persona que graba y recuenta sus muestras. */
    fun establecerAlias(nuevo: String) {
        alias = nuevo.trim().lowercase()
        disco.execute { _estado.update { it.copy(guardadas = contar(alias)) } }
    }

    fun grabar(sena: String) {
        val a = alias
        if (!FormatoGrabacion.ALIAS_VALIDO.matches(a)) {
            _estado.update { it.copy(mensaje = "Escribe un alias de 1 a 24 letras minúsculas, números, - o _ (no tu nombre).") }
            return
        }
        val repeticion = siguienteRepeticion(a, sena)
        synchronized(bloqueo) { grabador.iniciar(a, sena, repeticion) }
        _estado.update { it.copy(grabacion = grabador.estado, mensaje = null) }
    }

    fun cancelar() {
        synchronized(bloqueo) { grabador.cancelar() }
        _estado.update { it.copy(grabacion = EstadoGrabacion.Inactivo) }
    }

    /** Desde el hilo de análisis. Sin grabación activa no hace nada. */
    fun recibir(frame: LandmarkFrame) {
        val nuevo = synchronized(bloqueo) {
            val e = grabador.estado
            if (e !is EstadoGrabacion.CuentaAtras && e !is EstadoGrabacion.Grabando) return
            grabador.recibir(frame).also { if (it is EstadoGrabacion.Terminada || it is EstadoGrabacion.Fallida) grabador.reiniciar() }
        }
        when (nuevo) {
            is EstadoGrabacion.Terminada -> guardar(nuevo)
            else -> _estado.update { it.copy(grabacion = nuevo) }
        }
    }

    private fun guardar(terminada: EstadoGrabacion.Terminada) {
        _estado.update { it.copy(grabacion = terminada) }
        disco.execute {
            val m = terminada.muestra
            val mensaje = try {
                carpeta.mkdirs()
                File(carpeta, FormatoGrabacion.nombreArchivo(m)).writeText(FormatoGrabacion.escribir(m), Charsets.UTF_8)
                null
            } catch (e: IOException) {
                Log.e(TAG, "No se pudo guardar la grabación", e)
                "No se pudo guardar la grabación: ${e.message}"
            }
            _estado.update { it.copy(guardadas = contar(m.persona), mensaje = mensaje) }
        }
    }

    private fun archivosDe(alias: String): List<File> =
        carpeta.listFiles { f -> f.name.startsWith("${FormatoGrabacion.FUENTE}__${alias}__") && f.name.endsWith(".json") }?.toList().orEmpty()

    private fun contar(alias: String): Map<String, Int> =
        archivosDe(alias).groupingBy { it.name.split("__").getOrElse(2) { "" } }.eachCount()

    private fun siguienteRepeticion(alias: String, sena: String): Int =
        (archivosDe(alias).filter { it.name.split("__").getOrNull(2) == sena }
            .mapNotNull { it.name.substringAfterLast("__r").removeSuffix(".json").toIntOrNull() }
            .maxOrNull() ?: 0) + 1

    override fun close() {
        cancelar()
        disco.shutdown()
    }

    private companion object {
        const val TAG = "ControladorGrabacion"
    }
}
