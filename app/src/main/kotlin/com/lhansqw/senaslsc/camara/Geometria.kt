package com.lhansqw.senaslsc.camara

import kotlin.math.max

/** Ancho y alto de la imagen tras girarla [grados] (múltiplo de 90). */
fun dimensionesGiradas(ancho: Int, alto: Int, grados: Int): Pair<Int, Int> {
    require(grados % 90 == 0) { "La rotación debe ser múltiplo de 90: $grados" }
    return if ((grados / 90) % 2 == 0) ancho to alto else alto to ancho
}

/**
 * Pasa coordenadas normalizadas de la imagen analizada (0–1) a píxeles de la vista previa.
 *
 * La vista previa llena la pantalla recortando lo que sobra (como `ContentScale.Crop` centrado),
 * así que se aplica la misma escala y el mismo desplazamiento. Con la cámara frontal, la vista
 * previa se muestra en espejo y MediaPipe recibe la imagen sin espejo: por eso se invierte x.
 *
 * Requisito: la vista previa y el análisis deben tener la misma relación de aspecto (4:3), que
 * es lo que configura `ControladorCamara`.
 */
class TransformacionVista(
    anchoImagen: Int,
    altoImagen: Int,
    private val anchoVista: Float,
    private val altoVista: Float,
    private val espejo: Boolean,
) {
    init {
        require(anchoImagen > 0 && altoImagen > 0) { "Dimensiones de imagen no válidas" }
    }

    private val anchoEscalado: Float
    private val altoEscalado: Float
    private val desplazamientoX: Float
    private val desplazamientoY: Float

    init {
        val escala = max(anchoVista / anchoImagen, altoVista / altoImagen)
        anchoEscalado = anchoImagen * escala
        altoEscalado = altoImagen * escala
        desplazamientoX = (anchoVista - anchoEscalado) / 2f
        desplazamientoY = (altoVista - altoEscalado) / 2f
    }

    fun x(xNormalizada: Float): Float {
        val x = if (espejo) 1f - xNormalizada else xNormalizada
        return x * anchoEscalado + desplazamientoX
    }

    fun y(yNormalizada: Float): Float = yNormalizada * altoEscalado + desplazamientoY
}
