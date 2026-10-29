package com.lhansqw.senaslsc.recognizer

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.PrediccionSena
import com.lhansqw.senaslsc.core.model.Vocabulario

/**
 * Modelo visual que convierte una ventana de puntos de referencia en probabilidades de seña.
 *
 * No sabe nada del intérprete de texto ni de la interfaz: se puede reentrenar o sustituir
 * (otro modelo, otra lengua de señas) sin tocar el resto de la app.
 */
interface ReconocedorSenas : AutoCloseable {

    /** Señas que este modelo concreto sabe reconocer. */
    val vocabulario: Vocabulario

    /** Información del modelo cargado, para mostrarla en la app y en los informes. */
    val ficha: FichaModelo

    /** Número de fotogramas que espera cada ventana. */
    val fotogramasPorVentana: Int

    /**
     * Predice la seña de una ventana. La ventana debe tener exactamente
     * [fotogramasPorVentana] fotogramas en orden cronológico.
     */
    suspend fun predecir(ventana: List<LandmarkFrame>): PrediccionSena

    /** Libera el modelo y su memoria. Después no se puede volver a usar. */
    override fun close()
}

/** Datos del modelo que se publican junto con sus métricas (fase 6). */
data class FichaModelo(
    val nombre: String,
    val version: String,
    val lengua: String,
    /** Conjuntos de datos usados para entrenarlo, con su licencia. */
    val datosEntrenamiento: List<String>,
)

/** Comprueba la ventana antes de pasarla al modelo. Lo comparten todas las implementaciones. */
fun ReconocedorSenas.validarVentana(ventana: List<LandmarkFrame>) {
    require(ventana.size == fotogramasPorVentana) {
        "La ventana tiene ${ventana.size} fotogramas; el modelo espera $fotogramasPorVentana"
    }
    require(ventana.zipWithNext().all { (a, b) -> b.timestampMs >= a.timestampMs }) {
        "Los fotogramas de la ventana no están en orden cronológico"
    }
}
