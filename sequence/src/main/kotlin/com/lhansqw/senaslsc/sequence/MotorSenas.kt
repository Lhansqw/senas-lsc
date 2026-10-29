package com.lhansqw.senaslsc.sequence

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.PrediccionSena
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.core.model.Vocabulario

/** Lo que ocurre al recibir un fotograma. */
data class PasoFotograma(
    /** Ventana remuestreada lista para el modelo, o `null` si aún no toca predecir. */
    val ventana: List<LandmarkFrame>?,
    /** La secuencia se acaba de cerrar por una pausa sin manos. */
    val secuenciaCerrada: Boolean,
)

/** Lo que ocurre al recibir una predicción del modelo. */
data class PasoPrediccion(
    val confirmada: SenaConfirmada?,
    /** La secuencia se cerró por llegar al máximo de señas. */
    val secuenciaCerrada: Boolean,
)

/**
 * Etapas 4 a 6 de la arquitectura sin el modelo: fotogramas → ventana temporal → remuestreo, y
 * predicciones → filtro temporal → secuencia.
 *
 * Es síncrono y no conoce hilos ni corrutinas: la app lo usa desde un único consumidor y llama al
 * modelo entre [alRecibirFotograma] y [alRecibirPrediccion].
 */
class MotorSenas(
    vocabulario: Vocabulario,
    private val fotogramasModelo: Int,
    private val duracionVentanaMs: Long,
    pasoMs: Long = 250L,
    configuracionFiltro: ConfiguracionFiltro = ConfiguracionFiltro(),
    pausaCierreMs: Long = 1_200L,
    maximoSenas: Int = 20,
) {
    private val ventana = VentanaTemporal(duracionVentanaMs, pasoMs)
    private val filtro = FiltroTemporal(vocabulario, configuracionFiltro)
    private val secuencia = ConstructorSecuencia(pausaCierreMs, maximoSenas)

    val senas: List<SenaConfirmada> get() = secuencia.actual
    val cerrada: Boolean get() = secuencia.cerrada

    fun alRecibirFotograma(frame: LandmarkFrame): PasoFotograma {
        val cerradaPorPausa = secuencia.actualizar(frame.timestampMs, frame.hayManos)
        if (cerradaPorPausa) filtro.reiniciar()
        val lista = ventana.agregar(frame)?.let { Remuestreo.aTiempoUniforme(it, fotogramasModelo, duracionVentanaMs) }
        return PasoFotograma(lista, cerradaPorPausa)
    }

    fun alRecibirPrediccion(prediccion: PrediccionSena): PasoPrediccion {
        val confirmada = filtro.procesar(prediccion) ?: return PasoPrediccion(null, false)
        val llena = secuencia.agregar(confirmada)
        return PasoPrediccion(confirmada, llena)
    }

    /** El usuario pide interpretar lo que lleva. */
    fun cerrar(): Boolean = secuencia.cerrar()

    fun eliminarUltima(): SenaConfirmada? = secuencia.eliminarUltima()

    /** Borra la secuencia y el estado de las ventanas (al pausar o al pulsar «Limpiar»). */
    fun limpiar() {
        secuencia.limpiar()
        filtro.reiniciar()
        ventana.reiniciar()
    }
}
