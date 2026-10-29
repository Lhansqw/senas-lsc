package com.lhansqw.senaslsc.camara

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.camera.core.ImageProxy

/**
 * Convierte el fotograma de CameraX (RGBA, en la orientación del sensor) en un mapa de bits
 * vertical, tal como lo ve la cámara y **sin espejo**, que es lo que recibe MediaPipe.
 *
 * Por qué sin espejo: así «mano izquierda» significa la mano izquierda real de la persona, igual
 * que en los vídeos de los conjuntos de datos. El espejo solo se aplica al dibujar en pantalla.
 *
 * Reutiliza los mapas de bits entre fotogramas para no generar ~2 MB de basura por fotograma.
 * Se usa siempre desde el hilo de análisis; el mapa de bits devuelto solo es válido hasta la
 * siguiente llamada.
 */
class PreparadorImagen {

    private var origen: Bitmap? = null
    private var destino: Bitmap? = null
    private val matriz = Matrix()
    private val pintura = Paint(Paint.FILTER_BITMAP_FLAG)

    fun preparar(imagen: ImageProxy): Bitmap {
        val ancho = imagen.width
        val alto = imagen.height
        val plano = imagen.planes[0]

        // Camino rápido: filas sin relleno, se copian directamente en un mapa de bits reutilizado.
        // Si hay relleno al final de cada fila, CameraX hace la conversión (más lenta).
        val fuente = if (plano.pixelStride == 4 && plano.rowStride == ancho * 4) {
            reutilizar(origen, ancho, alto).also { bitmap ->
                origen = bitmap
                plano.buffer.rewind()
                bitmap.copyPixelsFromBuffer(plano.buffer)
            }
        } else {
            imagen.toBitmap()
        }

        val grados = imagen.imageInfo.rotationDegrees
        if (grados == 0) return fuente

        val (anchoFinal, altoFinal) = dimensionesGiradas(ancho, alto, grados)
        val girado = reutilizar(destino, anchoFinal, altoFinal).also { destino = it }
        matriz.reset()
        matriz.postTranslate(-ancho / 2f, -alto / 2f)
        matriz.postRotate(grados.toFloat())
        matriz.postTranslate(anchoFinal / 2f, altoFinal / 2f)
        Canvas(girado).drawBitmap(fuente, matriz, pintura)
        return girado
    }

    private fun reutilizar(actual: Bitmap?, ancho: Int, alto: Int): Bitmap =
        if (actual != null && actual.width == ancho && actual.height == alto) {
            actual
        } else {
            Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        }
}
