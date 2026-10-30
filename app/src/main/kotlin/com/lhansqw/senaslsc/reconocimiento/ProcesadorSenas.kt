package com.lhansqw.senaslsc.reconocimiento

import android.content.Context
import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.ia.ControladorIaLocal
import com.lhansqw.senaslsc.interpreter.ResultadoGenerativo
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
 * - Si hay IA local ([ia]), las frases cerradas se le piden en su propio hilo; mientras tanto el
 *   reconocimiento sigue, y el resultado se aplica de vuelta en este hilo.
 */
class ProcesadorSenas(context: Context, private val ia: ControladorIaLocal? = null) : AutoCloseable {

    private val contextoApp = context.applicationContext
    private val hilo = Executors.newSingleThreadExecutor { tarea -> Thread(tarea, "senas-reconocedor") }
    private val ambito = CoroutineScope(SupervisorJob() + hilo.asCoroutineDispatcher())
    private val fotogramas = Channel<LandmarkFrame>(CAPACIDAD, BufferOverflow.DROP_OLDEST)

    private var coordinador = CoordinadorReconocimiento(null, null, EstadoModeloSenas.Cargando)
    private var reconocedor: ReconocedorLiteRT? = null
    private var iaDisponible = false

    private val _estado = MutableStateFlow(EstadoReconocimiento())
    val estado: StateFlow<EstadoReconocimiento> = _estado.asStateFlow()

    init {
        ambito.launch {
            cargarModelo()
            for (frame in fotogramas) {
                coordinador.alRecibirFotograma(frame)
                publicar()
                atenderSolicitudes()
            }
        }
        ia?.let { controlador ->
            ambito.launch {
                controlador.estado.collect { estadoIa ->
                    iaDisponible = estadoIa.disponible
                    coordinador.refinamientoDisponible = iaDisponible
                }
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
    fun corregir(texto: String) = orden { coordinador.corregir(texto) }

    private fun orden(accion: suspend () -> Unit) {
        ambito.launch {
            accion()
            publicar()
            atenderSolicitudes()
        }
    }

    /** Si el coordinador dejó una frase para la IA, se la pide sin bloquear este hilo. */
    private fun atenderSolicitudes() {
        val solicitud = coordinador.tomarSolicitud() ?: return
        val controlador = ia ?: return
        val coordinadorDeLaSolicitud = coordinador
        ambito.launch {
            val resultado = controlador.interpretar(solicitud.senas)
                ?: ResultadoGenerativo.Descartada(
                    listOf("La IA local no está disponible."),
                    coordinadorDeLaSolicitud.estado.propuesta ?: return@launch,
                )
            // De vuelta en el hilo del reconocedor: se aplica solo si la frase no ha cambiado.
            coordinadorDeLaSolicitud.aplicarRefinamiento(solicitud.id, resultado)
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
        coordinador = CoordinadorReconocimiento(reconocedor, motor, estadoModelo).also {
            it.refinamientoDisponible = iaDisponible
        }
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
