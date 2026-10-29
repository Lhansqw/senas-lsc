package com.lhansqw.senaslsc.reconocimiento

import com.lhansqw.senaslsc.core.model.PrediccionSena
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import kotlin.math.roundToInt

/** 0.823 → "82 %" (en español va un espacio antes del signo). */
fun formatearPorcentaje(valor: Float): String = "${(valor.coerceIn(0f, 1f) * 100).roundToInt()} %"

/** "HOLA 91 %" para mostrar una seña confirmada. */
fun etiquetaSena(sena: SenaConfirmada): String = "${sena.entrada.glosa} ${formatearPorcentaje(sena.confianza)}"

/**
 * Texto de lo que el modelo está viendo ahora mismo, o `null` si no hay nada que mostrar.
 * Una predicción desconocida no muestra ninguna seña: no se sugiere lo que no se reconoce.
 */
fun textoPrediccionEnCurso(prediccion: PrediccionSena?, glosaDe: (String) -> String?): String? {
    if (prediccion == null || prediccion.desconocida) return null
    val mejor = prediccion.mejor ?: return null
    val glosa = glosaDe(mejor.id) ?: return null
    return "$glosa ${formatearPorcentaje(mejor.probabilidad)}"
}

/** Lista legible de las señas para lectores de pantalla: "HOLA, GRACIAS". */
fun listaParaLector(senas: List<SenaConfirmada>): String = senas.joinToString(", ") { it.entrada.glosa }

/** "Exactitud 86 % · F1 macro 81 %" con las métricas conocidas del modelo, en orden fijo. */
fun resumenMetricas(metricas: Map<String, Float>): String? {
    val nombres = linkedMapOf(
        "exactitud" to "Exactitud",
        "macro_f1" to "F1 macro",
        "rechazo_desconocidas" to "Rechazo de desconocidas",
    )
    val partes = nombres.mapNotNull { (clave, nombre) -> metricas[clave]?.let { "$nombre ${formatearPorcentaje(it)}" } }
    return partes.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
