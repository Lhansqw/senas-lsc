package com.lhansqw.senaslsc.core.model

/**
 * Punto de referencia normalizado.
 *
 * `x` e `y` van de 0 a 1 respecto al ancho y alto de la imagen; `z` es la profundidad relativa
 * que devuelve MediaPipe (negativa hacia la cámara).
 */
data class Punto3D(val x: Float, val y: Float, val z: Float)

/**
 * Un fotograma convertido en puntos de referencia. A partir de aquí ya no existe la imagen:
 * solo circulan coordenadas.
 */
data class LandmarkFrame(
    val timestampMs: Long,
    /** 21 puntos o `null` si la mano no se detecta. */
    val manoIzquierda: List<Punto3D>?,
    val manoDerecha: List<Punto3D>?,
    /** Subconjunto del torso superior (hombros, codos, muñecas, nariz) o `null`. */
    val cuerpo: List<Punto3D>?,
    /** Subconjunto del rostro relevante para expresiones no manuales, o `null`. */
    val rostro: List<Punto3D>?,
    /**
     * Ancho / alto de la imagen analizada. Las coordenadas x e y se normalizan por separado, así
     * que hace falta para medir distancias reales (en una imagen vertical 480×640 vale 0,75).
     */
    val relacionAspecto: Float = 1f,
) {
    init {
        require(timestampMs >= 0) { "timestampMs no puede ser negativo: $timestampMs" }
        require(relacionAspecto > 0f) { "La relación de aspecto debe ser positiva: $relacionAspecto" }
        manoIzquierda?.let { require(it.size == PUNTOS_POR_MANO) { "La mano izquierda debe tener $PUNTOS_POR_MANO puntos (tiene ${it.size})" } }
        manoDerecha?.let { require(it.size == PUNTOS_POR_MANO) { "La mano derecha debe tener $PUNTOS_POR_MANO puntos (tiene ${it.size})" } }
    }

    val hayManos: Boolean get() = manoIzquierda != null || manoDerecha != null

    companion object {
        /** Puntos que MediaPipe devuelve por mano. */
        const val PUNTOS_POR_MANO = 21
    }
}
