package com.lhansqw.senaslsc.vision

/**
 * Decide en qué fotogramas se ejecuta Pose Landmarker. En los demás se reutiliza la última pose.
 *
 * La pose solo sirve para saber qué mano es cuál (por las muñecas) y para los puntos del cuerpo, que
 * se mueven mucho más despacio que las manos. En un teléfono de gama baja es la mitad del tiempo de
 * MediaPipe: ejecutarla uno de cada [cada] fotogramas sube los FPS sin cambiar de modelo.
 *
 * Se ejecuta siempre si todavía no hay pose o si cambió el número de manos, para que la asignación
 * izquierda/derecha no se quede con la pose de antes de que entrara o saliera una mano.
 *
 * Kotlin puro y sin sincronización: usar desde un solo hilo.
 */
class PlanificadorPose(private val cada: Int) {
    init {
        require(cada >= 1) { "cada debe ser 1 o más" }
    }

    private var restantes = 0
    private var manosAnteriores = -1

    /**
     * @param manos manos detectadas en este fotograma.
     * @param hayPose hay una pose anterior que se pueda reutilizar.
     * @param siempre ejecutarla en este fotograma pase lo que pase (al grabar muestras).
     */
    fun debeEjecutar(manos: Int, hayPose: Boolean, siempre: Boolean = false): Boolean {
        val toca = siempre || !hayPose || manos != manosAnteriores || restantes <= 0
        manosAnteriores = manos
        restantes = if (toca) cada - 1 else restantes - 1
        return toca
    }
}
