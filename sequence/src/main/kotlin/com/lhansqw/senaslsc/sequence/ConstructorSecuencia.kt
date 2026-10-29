package com.lhansqw.senaslsc.sequence

import com.lhansqw.senaslsc.core.model.SenaConfirmada

/**
 * Acumula las señas confirmadas y cierra la secuencia tras una pausa.
 *
 * La pausa (manos fuera de cámara o en reposo durante [pausaCierreMs]) marca el final de una
 * frase. Es la forma de segmentar del MVP: el señado continuo sin pausas queda fuera de alcance
 * (doc. 01).
 */
class ConstructorSecuencia(
    private val pausaCierreMs: Long = 1_200L,
    private val maximoSenas: Int = 20,
) {
    init {
        require(pausaCierreMs > 0) { "La pausa de cierre debe ser positiva" }
        require(maximoSenas > 0) { "El máximo de señas debe ser positivo" }
    }

    private val senas = mutableListOf<SenaConfirmada>()
    private var ultimaActividadMs = 0L

    /** Señas de la secuencia actual, en orden. */
    val actual: List<SenaConfirmada> get() = senas.toList()

    /** `true` cuando la secuencia terminó y está lista para el intérprete. */
    var cerrada: Boolean = false
        private set

    /**
     * Añade una seña. Si la secuencia anterior ya estaba cerrada, empieza una nueva.
     * @return `true` si la secuencia llegó al máximo y se cerró.
     */
    fun agregar(sena: SenaConfirmada): Boolean {
        if (cerrada) {
            senas.clear()
            cerrada = false
        }
        senas += sena
        ultimaActividadMs = maxOf(ultimaActividadMs, sena.finMs)
        if (senas.size >= maximoSenas) cerrada = true
        return cerrada
    }

    /**
     * Informa del estado de la cámara en cada fotograma o ventana.
     * @return `true` si la secuencia se acaba de cerrar por pausa.
     */
    fun actualizar(ahoraMs: Long, hayManos: Boolean): Boolean {
        if (hayManos) {
            ultimaActividadMs = maxOf(ultimaActividadMs, ahoraMs)
            return false
        }
        if (cerrada || senas.isEmpty()) return false
        if (ahoraMs - ultimaActividadMs >= pausaCierreMs) {
            cerrada = true
            return true
        }
        return false
    }

    /** Cierra la secuencia a petición del usuario (botón «Interpretar»). */
    fun cerrar(): Boolean {
        if (senas.isEmpty()) return false
        cerrada = true
        return true
    }

    /** Quita la última seña (corrección del usuario). La secuencia vuelve a quedar abierta. */
    fun eliminarUltima(): SenaConfirmada? {
        val quitada = senas.removeLastOrNull()
        cerrada = false
        return quitada
    }

    fun limpiar() {
        senas.clear()
        cerrada = false
        ultimaActividadMs = 0L
    }
}
