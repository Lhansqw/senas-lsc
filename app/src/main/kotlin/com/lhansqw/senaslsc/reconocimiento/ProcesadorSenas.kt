package com.lhansqw.senaslsc.reconocimiento

import android.content.Context
import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.recognizer.litert.CargadorReconocedor
import com.lhansqw.senaslsc.recognizer.litert.ReconocedorLiteRT
import com.lhansqw.senaslsc.recognizer.litert.ResultadoCarga
import com.lhansqw.senaslsc.sequence.MotorSenas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Ejecuta el reconocimiento en un hilo propio («senas-reconocedor»), separado del hilo de la
 * cámara y de MediaPipe:
 *
 * - LiteRT exige crear, usar y cerrar el modelo desde el mismo hilo de fondo.
 * - Si el modelo va más lento que la cámara, el canal descarta los fotogramas más viejos
 *   (capacidad [CAPACIDAD], DROP_OLDEST): se trabaja siempre con lo reciente y la latencia no crece.
 * - Las órdenes del usuario (interpretar, borrar, limpiar) se ejecutan en el mismo hilo, entre
 *   fotograma y fotograma, así que nunca hay dos accesos simultáneos al coordinador.
 */
class ProcesadorSenas(context: Context) : AutoCloseable {

    private val contextoApp = context.applicationContext
    private val hilo = Executors.newSingleThreadExecutor { tarea -> Thread(tarea, "senas-reconocedor") }
    private val ambito = CoroutineScope(SupervisorJob() + hilo.asCoroutineDispatcher())
    private val fotogramas = Channel<LandmarkFrame>(CAPACIDAD, BufferOverflow.DROP_OLDEST)

    private var coordinador = CoordinadorReconocimiento(null, null, EstadoModeloSenas.Cargando)
    private var reconocedor: ReconocedorLiteRT? = null

    private val _estado = MutableStateFlow(EstadoReconocimiento())
    val estado: StateFlow<EstadoReconocimiento> = _estado.asStateFlow()

    init {
        ambito.launch {
            cargarModelo()
            for (frame in fotogramas) {
                coordinador.alRecibirFotograma(frame)
                publicar()
            }
        }
    }

    /** Se llama desde el hilo de análisis por cada fotograma procesado por MediaPipe. */
    fun enviarFotograma(frame: LandmarkFrame) {
        fotogramas.trySend(frame)
    }

    fun interpretar() = orden { coordinador.interpretar() }
    fun eliminarUltima() = orden { coordinador.eliminarUltima() }
    fun limpiar() = orden { coordinador.limpiar() }

    private fun orden(accion: suspend () -> Unit) {
        ambito.launch {
            accion()
            publicar()
        }
    }

    private fun cargarModelo() {
        val (estadoModelo, motor) = when (val r = CargadorReconocedor.cargar(contextoApp)) {
            is ResultadoCarga.Cargado -> {
                reconocedor = r.reconocedor
                val c = r.contrato
                EstadoModeloSenas.Listo(c.nombre, c.version, c.entradas.size, c.metricas, c.entradas.associate { it.id to it.glosa }) to
                    MotorSenas(r.reconocedor.vocabulario, c.fotogramas, c.duracionVentanaMs)
            }
            ResultadoCarga.SinModelo -> EstadoModeloSenas.SinModelo to null
            is ResultadoCarga.Invalido -> EstadoModeloSenas.Invalido(r.errores) to null
            is ResultadoCarga.Fallo -> EstadoModeloSenas.Fallo(r.mensaje) to null
        }
        coordinador = CoordinadorReconocimiento(reconocedor, motor, estadoModelo)
        publicar()
    }

    private fun publicar() {
        _estado.value = coordinador.estado
    }

    override fun close() {
        fotogramas.close()
        // El modelo se cierra en su propio hilo; después se detiene el hilo.
        ambito.launch { reconocedor?.close() }.invokeOnCompletion {
            ambito.cancel()
            hilo.shutdown()
        }
    }

    private companion object {
        /** Unos 250 ms de fotogramas a 30 FPS. */
        const val CAPACIDAD = 8
    }
}
