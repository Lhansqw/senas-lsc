package com.lhansqw.senaslsc.interpreter

import kotlin.math.abs
import kotlin.math.roundToLong

/** 0.58 → "0,58" (formato que se muestra al usuario, español de Colombia). */
fun formatearConfianza(confianza: Float): String = dosDecimales(confianza, ',')

/** 0.58 → "0.58" (formato para el modelo de lenguaje, sin depender del idioma del teléfono). */
internal fun formatearConfianzaMaquina(confianza: Float): String = dosDecimales(confianza, '.')

/**
 * Dos decimales con el separador indicado y redondeo a la mitad hacia arriba, como `%.2f`.
 * Se hace a mano porque `String.format` solo existe en la JVM y este código también corre en iOS.
 */
private fun dosDecimales(valor: Float, separador: Char): String {
    if (!valor.isFinite()) return valor.toString()
    val centesimas = (abs(valor.toDouble()) * 100).roundToLong()
    val signo = if (valor < 0 && centesimas != 0L) "-" else ""
    val decimales = (centesimas % 100).toString().padStart(2, '0')
    return "$signo${centesimas / 100}$separador$decimales"
}
