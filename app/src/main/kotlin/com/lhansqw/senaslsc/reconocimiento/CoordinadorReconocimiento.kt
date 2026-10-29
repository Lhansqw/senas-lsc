package com.lhansqw.senaslsc.reconocimiento

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.interpreter.rules.InterpreteReglas
import com.lhansqw.senaslsc.recognizer.ReconocedorSenas
import com.lhansqw.senaslsc.sequence.MotorSenas

/**
 * Une el reconocedor visual, el motor de secuencias y el intérprete en un estado único.
 *
 * Kotlin puro y sin hilos: [ProcesadorSenas] lo usa desde un único hilo y publica [estado].
 * Así toda la lógica que decide qué ve el usuario se prueba en la JVM con un reconocedor falso.
 *
 * @param proponer convierte una secuencia cerrada en una propuesta. Por defecto, el intérprete por
 *   reglas; la fase 8 añade el modelo de lenguaje.
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
            if (resultado.confirmada != null) {
                // Una seña nueva empieza o amplía una secuencia: la propuesta anterior ya no aplica.
                estado = estado.copy(propuesta = if (estado.secuenciaCerrada) null else estado.propuesta)
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
        estado = estado.copy(propuesta = null)
        sincronizar()
    }

    fun limpiar() {
        motor?.limpiar()
        estado = estado.copy(ultimaPrediccion = null, propuesta = null)
        sincronizar()
    }

    private fun sincronizar() {
        val m = motor ?: return
        estado = estado.copy(senas = m.senas, secuenciaCerrada = m.cerrada)
    }

    private suspend fun generarPropuesta() {
        val senas = estado.senas
        if (senas.isEmpty()) return
        estado = estado.copy(propuesta = proponer(senas))
    }
}
