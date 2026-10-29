package com.lhansqw.senaslsc.ia

import android.content.Context
import android.util.Log
import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.interpreter.InterpreteGenerativo
import com.lhansqw.senaslsc.interpreter.ResultadoGenerativo
import com.lhansqw.senaslsc.interpreter.litertlm.MotorLiteRtLm
import com.lhansqw.senaslsc.interpreter.rules.InterpreteReglas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Carga y usa el modelo de lenguaje del teléfono en un hilo propio («senas-ia»).
 *
 * El modelo ocupa del orden de 1,5–2 GB de RAM, así que solo se carga mientras la pantalla de
 * reconocimiento está abierta, y se libera al cerrarla.
 *
 * @param rutaModelo ruta del archivo instalado, o `null` si no lo está.
 */
class ControladorIaLocal(
    context: Context,
    compatible: Boolean,
    motivoNoCompatible: String?,
    rutaModelo: String?,
    activadaPorUsuario: Boolean,
) : AutoCloseable {

    private val directorioCache = context.applicationContext.cacheDir.absolutePath
    private val hilo = Executors.newSingleThreadExecutor { tarea -> Thread(tarea, "senas-ia") }
    private val despachador = hilo.asCoroutineDispatcher()
    private val ambito = CoroutineScope(SupervisorJob() + despachador)

    private var motor: MotorLiteRtLm? = null
    private var interprete: InterpreteGenerativo? = null

    private val _estado = MutableStateFlow(decidirEstadoIa(compatible, motivoNoCompatible, rutaModelo != null, activadaPorUsuario))
    val estado: StateFlow<EstadoIaLocal> = _estado.asStateFlow()

    init {
        if (_estado.value == EstadoIaLocal.Cargando && rutaModelo != null) {
            ambito.launch { cargar(rutaModelo) }
        }
    }

    private fun cargar(ruta: String) {
        _estado.value = try {
            val m = MotorLiteRtLm.crear(ruta, directorioCache)
            motor = m
            interprete = InterpreteGenerativo(m, OrigenInterpretacion.LOCAL, InterpreteReglas())
            EstadoIaLocal.Lista
        } catch (e: Throwable) {
            // Incluye OutOfMemoryError: un teléfono sin memoria libre no debe cerrar la app.
            Log.e(TAG, "No se pudo cargar el modelo de lenguaje", e)
            EstadoIaLocal.Error("No se pudo cargar el modelo de lenguaje: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Pide una frase al modelo para [senas]. Se ejecuta en el hilo de la IA; quien llama puede
     * seguir trabajando mientras tanto.
     */
    suspend fun interpretar(senas: List<SenaConfirmada>): ResultadoGenerativo? = withContext(despachador) {
        interprete?.interpretar(senas)
    }

    override fun close() {
        ambito.launch { motor?.close() }.invokeOnCompletion {
            ambito.cancel()
            hilo.shutdown()
        }
    }

    private companion object {
        const val TAG = "ControladorIaLocal"
    }
}
