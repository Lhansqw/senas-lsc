package com.lhansqw.senaslsc.reconocimiento

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.interpreter.ReglasInterpretacion
import com.lhansqw.senaslsc.interpreter.ResultadoGenerativo
import com.lhansqw.senaslsc.interpreter.rules.InterpreteReglas
import com.lhansqw.senaslsc.recognizer.ReconocedorSenas
import com.lhansqw.senaslsc.sequence.MotorSenas

/**
 * Une el reconocedor visual, el motor de secuencias y el intérprete en un estado único.
 *
 * Kotlin puro y sin hilos: [ProcesadorSenas] lo usa desde un único hilo y publica [estado].
 * Así toda la lógica que decide qué ve el usuario se prueba en la JVM con un reconocedor falso.
 *
 * @param proponer convierte una secuencia cerrada en una propuesta inmediata (por defecto, el
 *   intérprete por reglas).
 *
 * ## Mejora con el modelo de lenguaje
 * Si [refinamientoDisponible] es `true`, al cerrarse una secuencia de 2 o más señas queda una
 * [SolicitudRefinamiento] pendiente. Quien use el coordinador la recoge con [tomarSolicitud],
 * consulta al modelo en otro hilo (tarda segundos) y entrega el resultado con
 * [aplicarRefinamiento]. El resultado solo se aplica si la secuencia sigue siendo la misma: si el
 * usuario ya borró o añadió señas, se descarta.
 */
class CoordinadorReconocimiento(
    private val reconocedor: ReconocedorSenas?,
    private val motor: MotorSenas?,
    modeloInicial: EstadoModeloSenas,
    private val proponer: suspend (List<SenaConfirmada>) -> PropuestaFrase = { InterpreteReglas().componer(it) },
) {
    init {
        require((reconocedor == null) == (motor == null)) { "Reconocedor y motor van juntos" }
    }

    var estado: EstadoReconocimiento = EstadoReconocimiento(modelo = modeloInicial)
        private set

    /** Hay un modelo de lenguaje listo y el usuario quiere usarlo. */
    var refinamientoDisponible: Boolean = false

    private var pendiente: SolicitudRefinamiento? = null
    private var ultimaSolicitud: SolicitudRefinamiento? = null
    private var contadorSolicitudes = 0L

    /** Devuelve la solicitud pendiente (si hay) y la marca como tomada. */
    fun tomarSolicitud(): SolicitudRefinamiento? = pendiente.also { pendiente = null }

    /** Entrega el resultado del modelo de lenguaje para la solicitud [id]. */
    fun aplicarRefinamiento(id: Long, resultado: ResultadoGenerativo) {
        val solicitud = ultimaSolicitud?.takeIf { it.id == id } ?: return
        ultimaSolicitud = null
        val vigente = estado.secuenciaCerrada && estado.senas.map { it.id } == solicitud.senas.map { it.id }
        estado = estado.copy(generando = false)
        if (!vigente) return
        estado = when (resultado) {
            is ResultadoGenerativo.Aceptada -> estado.copy(propuesta = resultado.propuesta, motivoDescarteIa = null)
            is ResultadoGenerativo.Descartada -> estado.copy(motivoDescarteIa = resultado.motivos.firstOrNull())
            is ResultadoGenerativo.NoAplica -> estado
        }
    }

    /** Procesa un fotograma: puede producir una predicción, confirmar una seña o cerrar la frase. */
    suspend fun alRecibirFotograma(frame: LandmarkFrame) {
        val m = motor ?: return
        val r = reconocedor ?: return

        val paso = m.alRecibirFotograma(frame)
        var cerrada = paso.secuenciaCerrada
        paso.ventana?.let { ventana ->
            val prediccion = r.predecir(ventana)
            val resultado = m.alRecibirPrediccion(prediccion)
            estado = estado.copy(ultimaPrediccion = prediccion)
            if (resultado.confirmada != null && estado.secuenciaCerrada) {
                // Una seña nueva tras cerrar empieza otra frase: la propuesta anterior ya no aplica.
                descartarPropuesta()
            }
            cerrada = cerrada || resultado.secuenciaCerrada
        }
        if (!frame.hayManos && estado.ultimaPrediccion != null) {
            estado = estado.copy(ultimaPrediccion = null)
        }
        sincronizar()
        if (cerrada) generarPropuesta()
    }

    /** El usuario pulsa «Interpretar». */
    suspend fun interpretar() {
        val m = motor ?: return
        if (m.cerrar()) {
            sincronizar()
            generarPropuesta()
        }
    }

    fun eliminarUltima() {
        motor?.eliminarUltima() ?: return
        descartarPropuesta()
        sincronizar()
    }

    fun limpiar() {
        motor?.limpiar()
        descartarPropuesta()
        estado = estado.copy(ultimaPrediccion = null)
        sincronizar()
    }

    private fun descartarPropuesta() {
        pendiente = null
        ultimaSolicitud = null
        estado = estado.copy(propuesta = null, generando = false, motivoDescarteIa = null)
    }

    private fun sincronizar() {
        val m = motor ?: return
        estado = estado.copy(senas = m.senas, secuenciaCerrada = m.cerrada)
    }

    private suspend fun generarPropuesta() {
        val senas = estado.senas
        if (senas.isEmpty()) return
        estado = estado.copy(propuesta = proponer(senas), motivoDescarteIa = null)
        if (refinamientoDisponible && senas.size >= ReglasInterpretacion.MINIMO_SENAS_MODELO) {
            val solicitud = SolicitudRefinamiento(++contadorSolicitudes, senas)
            pendiente = solicitud
            ultimaSolicitud = solicitud
            estado = estado.copy(generando = true)
        }
    }
}

/** Petición de mejorar la propuesta de una secuencia con el modelo de lenguaje. */
data class SolicitudRefinamiento(val id: Long, val senas: List<SenaConfirmada>)
