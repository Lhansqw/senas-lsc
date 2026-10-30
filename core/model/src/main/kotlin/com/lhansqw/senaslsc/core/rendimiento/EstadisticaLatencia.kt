package com.lhansqw.senaslsc.core.rendimiento

/** Percentiles de las últimas latencias medidas, en milisegundos. */
data class ResumenLatencia(
    val p50Ms: Float,
    val p95Ms: Float,
    val maximaMs: Float,
    /** Cuántas mediciones se usaron (como mucho, la capacidad de la estadística). */
    val muestras: Int,
)

/**
 * Guarda las últimas [capacidad] latencias y calcula sus percentiles (fase 11).
 *
 * La media esconde los tirones: lo que nota el usuario es el p95 (el 5 % de veces más lentas).
 * Percentil por rango más cercano, sin interpolar: el valor devuelto siempre es una medición real.
 * Kotlin puro y sin sincronización: usar desde un solo hilo.
 */
class EstadisticaLatencia(private val capacidad: Int = 300) {

    init {
        require(capacidad > 0) { "La capacidad debe ser positiva" }
    }

    private val valores = ArrayDeque<Float>()

    val vacia: Boolean get() = valores.isEmpty()

    fun registrar(ms: Float) {
        require(ms >= 0f && ms.isFinite()) { "Latencia no válida: $ms" }
        valores.addLast(ms)
        if (valores.size > capacidad) valores.removeFirst()
    }

    fun registrarNanos(nanos: Long) = registrar(nanos / 1_000_000f)

    fun resumen(): ResumenLatencia? {
        if (valores.isEmpty()) return null
        val ordenados = valores.sorted()
        return ResumenLatencia(percentil(ordenados, 50.0), percentil(ordenados, 95.0), ordenados.last(), ordenados.size)
    }

    fun reiniciar() = valores.clear()

    companion object {
        /** Percentil [p] (0–100] de una lista ya ordenada, por rango más cercano. */
        fun percentil(ordenados: List<Float>, p: Double): Float {
            require(ordenados.isNotEmpty()) { "No hay valores" }
            require(p > 0.0 && p <= 100.0) { "El percentil debe estar entre 0 y 100" }
            val rango = kotlin.math.ceil(p / 100.0 * ordenados.size).toInt().coerceIn(1, ordenados.size)
            return ordenados[rango - 1]
        }
    }
}
