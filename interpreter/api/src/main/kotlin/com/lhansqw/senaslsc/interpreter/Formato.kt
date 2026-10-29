package com.lhansqw.senaslsc.interpreter

import java.util.Locale

private val ESPANOL_COLOMBIA: Locale = Locale.forLanguageTag("es-CO")

/** 0.58 → "0,58" (formato que se muestra al usuario). */
fun formatearConfianza(confianza: Float): String = String.format(ESPANOL_COLOMBIA, "%.2f", confianza)

/** 0.58 → "0.58" (formato para el modelo de lenguaje, sin depender del idioma del teléfono). */
internal fun formatearConfianzaMaquina(confianza: Float): String = String.format(Locale.ROOT, "%.2f", confianza)
