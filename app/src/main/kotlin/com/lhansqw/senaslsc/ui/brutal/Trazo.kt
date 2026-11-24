package com.lhansqw.senaslsc.ui.brutal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.ui.theme.ColoresBrutal
import com.lhansqw.senaslsc.ui.theme.LocalBrutal

/**
 * El bloque brutalista: fondo plano, borde de trazo y sombra dura desplazada abajo a la derecha.
 * Reserva el espacio de la sombra con un margen, así nada se solapa con lo que viene después.
 *
 * @param hundido el bloque baja hasta tapar su sombra (botón presionado).
 * @param foco contorno extra por fuera, para el foco de teclado o D-pad.
 */
fun Modifier.bloqueBrutal(
    fondo: Color,
    trazo: Color,
    grosor: Dp,
    sombra: Dp,
    hundido: Boolean = false,
    foco: Color? = null,
    conSombra: Boolean = true,
): Modifier = padding(end = sombra, bottom = sombra).drawBehind {
    val g = grosor.toPx()
    val s = sombra.toPx()
    val origen = if (hundido) Offset(s, s) else Offset.Zero
    if (conSombra && !hundido && s > 0f) drawRect(trazo, topLeft = Offset(s, s), size = size)
    if (foco != null) {
        // Por fuera del borde, para que se vea aunque el fondo sea del mismo color.
        val f = g * 2
        drawRect(foco, topLeft = origen - Offset(f, f), size = Size(size.width + f * 2, size.height + f * 2), style = Stroke(g))
    }
    drawRect(fondo, topLeft = origen, size = size)
    drawRect(trazo, topLeft = origen + Offset(g / 2, g / 2), size = Size(size.width - g, size.height - g), style = Stroke(g))
}

/** Línea de trazo a todo el ancho, para separar filas de una tabla o lista. */
@Composable
fun Separador(modifier: Modifier = Modifier, grosor: Dp = 2.dp) {
    Box(modifier.fillMaxWidth().height(grosor).background(LocalBrutal.current.trazo))
}

/** Etiqueta pequeña con borde: estado, tipo de seña, versión. */
@Composable
fun Insignia(texto: String, modifier: Modifier = Modifier, color: Color? = null) {
    val brutal = LocalBrutal.current
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = if (color != null) ColoresBrutal.Tinta else brutal.texto,
        modifier = modifier
            .background(color ?: brutal.superficie)
            .border(2.dp, if (color != null) ColoresBrutal.Tinta else brutal.trazo)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
