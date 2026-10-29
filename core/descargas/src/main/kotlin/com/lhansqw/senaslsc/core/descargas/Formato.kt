package com.lhansqw.senaslsc.core.descargas

import java.util.Locale
import kotlin.math.roundToLong

private val ES: Locale = Locale.forLanguageTag("es-CO")

private const val KB = 1024L
private const val MB = KB * 1024
private const val GB = MB * 1024

/** 2_708_000_000 → "2,5 GB"; 734_003_200 → "700 MB"; 2_048 → "2 KB". Unidades binarias. */
fun formatearTamano(bytes: Long): String {
    require(bytes >= 0) { "El tamaño no puede ser negativo" }
    return when {
        bytes >= GB -> String.format(ES, "%.1f GB", bytes.toDouble() / GB)
        bytes >= MB -> "${(bytes.toDouble() / MB).roundToLong()} MB"
        bytes >= KB -> "${(bytes.toDouble() / KB).roundToLong()} KB"
        else -> "$bytes B"
    }
}

/** 200 → "3 min 20 s"; 45 → "45 s"; 3_700 → "1 h 2 min". */
fun formatearDuracion(segundos: Long): String {
    require(segundos >= 0) { "La duración no puede ser negativa" }
    val h = segundos / 3600
    val min = (segundos % 3600) / 60
    val s = segundos % 60
    return when {
        h > 0 -> if (min > 0) "$h h $min min" else "$h h"
        min > 0 -> if (s > 0) "$min min $s s" else "$min min"
        else -> "$s s"
    }
}

/** Velocidad legible: 1_572_864 B/s → "1,5 MB/s". */
fun formatearVelocidad(bytesPorSegundo: Double): String {
    require(bytesPorSegundo >= 0) { "La velocidad no puede ser negativa" }
    return when {
        bytesPorSegundo >= MB -> String.format(ES, "%.1f MB/s", bytesPorSegundo / MB)
        bytesPorSegundo >= KB -> "${(bytesPorSegundo / KB).roundToLong()} KB/s"
        else -> "${bytesPorSegundo.roundToLong()} B/s"
    }
}
