package com.lhansqw.senaslsc.core.descargas

/** Fotografía del avance de una descarga o una copia. */
data class Progreso(
    val bytes: Long,
    /** Total esperado, o `null` si el servidor no lo informó. */
    val total: Long?,
    /** Velocidad media reciente en bytes por segundo. */
    val velocidad: Double,
) {
    /** Fracción completada (0–1), o `null` si no se conoce el total. */
    val fraccion: Float? get() = total?.takeIf { it > 0 }?.let { (bytes.toDouble() / it).coerceIn(0.0, 1.0).toFloat() }

    /** Segundos restantes estimados, o `null` si no se pueden calcular. */
    val segundosRestantes: Long?
        get() {
            val t = total ?: return null
            if (velocidad <= 0.0 || bytes >= t) return null
            return ((t - bytes) / velocidad).toLong()
        }
}

/**
 * Calcula la velocidad sobre una ventana deslizante de tiempo, para que la estimación no salte con
 * cada bloque recibido. Kotlin puro: el reloj se pasa como parámetro.
 */
class MedidorProgreso(
    private val total: Long?,
    inicialBytes: Long = 0,
    private val ventanaMs: Long = 5_000,
) {
    init {
        require(inicialBytes >= 0) { "inicialBytes no puede ser negativo" }
        require(ventanaMs > 0) { "La ventana debe ser positiva" }
    }

    private val muestras = ArrayDeque<Pair<Long, Long>>() // (milisegundos, bytes acumulados)
    var bytes: Long = inicialBytes
        private set

    fun registrar(ahoraMs: Long, bytesAcumulados: Long): Progreso {
        require(bytesAcumulados >= bytes) { "Los bytes acumulados no pueden disminuir" }
        bytes = bytesAcumulados
        muestras.addLast(ahoraMs to bytesAcumulados)
        while (muestras.size > 2 && ahoraMs - muestras.first().first > ventanaMs) muestras.removeFirst()
        return instantanea()
    }

    fun instantanea(): Progreso {
        val velocidad = if (muestras.size >= 2) {
            val (t0, b0) = muestras.first()
            val (t1, b1) = muestras.last()
            if (t1 > t0) (b1 - b0) * 1000.0 / (t1 - t0) else 0.0
        } else {
            0.0
        }
        return Progreso(bytes, total, velocidad)
    }
}

/** Comprobación del espacio libre antes de descargar o copiar un archivo grande. */
object EspacioNecesario {

    /** Margen de seguridad: 10 % del tamaño y, como mínimo, 200 MB para el resto del sistema. */
    const val MARGEN_MINIMO = 200L * 1024 * 1024

    /** Bytes libres necesarios para completar un archivo de [total] del que ya hay [descargados]. */
    fun requerido(total: Long, descargados: Long = 0): Long {
        require(total >= 0 && descargados >= 0) { "Los tamaños no pueden ser negativos" }
        val restante = (total - descargados).coerceAtLeast(0)
        return restante + maxOf(MARGEN_MINIMO, total / 10)
    }

    fun suficiente(libres: Long, total: Long, descargados: Long = 0): Boolean = libres >= requerido(total, descargados)
}
