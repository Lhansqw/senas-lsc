package com.lhansqw.senaslsc.camara

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.core.model.Punto3D
import com.lhansqw.senaslsc.vision.Subconjuntos

// Colores distintos para cada mano; se distinguen también por la posición, no solo por el color.
private val COLOR_MANO_IZQUIERDA = Color(0xFF4CDADC)
private val COLOR_MANO_DERECHA = Color(0xFFFFB74D)
private val COLOR_CUERPO = Color(0xCCFFFFFF)
private val COLOR_ROSTRO = Color(0xCCAAC7FF)

/**
 * Dibuja sobre la vista previa los puntos que MediaPipe extrajo del último fotograma.
 * Es decorativa: la información equivalente para lectores de pantalla está en el panel de métricas.
 */
@Composable
fun CapaLandmarks(resultado: ResultadoLandmarks?, espejo: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = resultado ?: return@Canvas
        val t = TransformacionVista(r.anchoImagen, r.altoImagen, size.width, size.height, espejo)
        val f = r.frame

        f.cuerpo?.let { dibujarConexiones(it, Subconjuntos.CONEXIONES_CUERPO, t, COLOR_CUERPO, 3.dp.toPx()) }
        f.rostro?.let { puntos -> puntos.forEach { dibujarPunto(it, t, COLOR_ROSTRO, 2.dp.toPx()) } }
        f.manoIzquierda?.let { dibujarMano(it, t, COLOR_MANO_IZQUIERDA) }
        f.manoDerecha?.let { dibujarMano(it, t, COLOR_MANO_DERECHA) }
    }
}

private fun DrawScope.dibujarMano(puntos: List<Punto3D>, t: TransformacionVista, color: Color) {
    dibujarConexiones(puntos, Subconjuntos.CONEXIONES_MANO, t, color, 3.dp.toPx())
    puntos.forEach { dibujarPunto(it, t, color, 4.dp.toPx()) }
}

private fun DrawScope.dibujarConexiones(
    puntos: List<Punto3D>,
    conexiones: List<Pair<Int, Int>>,
    t: TransformacionVista,
    color: Color,
    grosor: Float,
) {
    conexiones.forEach { (a, b) ->
        val pa = puntos[a]
        val pb = puntos[b]
        drawLine(color, Offset(t.x(pa.x), t.y(pa.y)), Offset(t.x(pb.x), t.y(pb.y)), grosor, StrokeCap.Round)
    }
}

private fun DrawScope.dibujarPunto(p: Punto3D, t: TransformacionVista, color: Color, radio: Float) {
    drawCircle(color, radio, Offset(t.x(p.x), t.y(p.y)))
}
