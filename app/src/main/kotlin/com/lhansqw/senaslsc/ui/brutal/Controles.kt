package com.lhansqw.senaslsc.ui.brutal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.ui.theme.ColoresBrutal
import com.lhansqw.senaslsc.ui.theme.LocalBrutal
import com.lhansqw.senaslsc.ui.theme.TemaSenasLSC

/**
 * Campo de texto con la etiqueta encima (no flotante) y un borde grueso que cambia al color de foco.
 * TalkBack lee la etiqueta como descripción del campo.
 */
@Composable
fun CampoBrutal(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val brutal = LocalBrutal.current
    val interacciones = remember { MutableInteractionSource() }
    val enfocado by interacciones.collectIsFocusedAsState()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = brutal.texto, modifier = Modifier.clearAndSetSemantics {})
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            interactionSource = interacciones,
            shape = RectangleShape,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                // El borde lo dibuja el modificador: el de Material es de 1–2 dp.
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = brutal.superficie,
                unfocusedContainerColor = brutal.superficie,
                focusedTextColor = brutal.texto,
                unfocusedTextColor = brutal.texto,
                cursorColor = brutal.texto,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(brutal.grosor, if (enfocado) brutal.foco else brutal.trazo)
                .semantics { contentDescription = etiqueta },
        )
    }
}

/**
 * Interruptor cuadrado. Sin [onCambio] no recibe toques: lo maneja la fila que lo contiene
 * (ver [FilaInterruptor]), que es la que lleva la semántica.
 */
@Composable
fun InterruptorBrutal(activo: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onCambio: ((Boolean) -> Unit)? = null) {
    val brutal = LocalBrutal.current
    val trazo = if (enabled) brutal.trazo else brutal.textoSuave
    val toque = if (onCambio != null) {
        Modifier
            .minimumInteractiveComponentSize()
            .toggleable(value = activo, enabled = enabled, role = Role.Switch, onValueChange = onCambio)
    } else {
        Modifier
    }
    Box(
        modifier
            .then(toque)
            .size(width = 56.dp, height = 32.dp)
            .background(if (activo && enabled) ColoresBrutal.Cian else brutal.superficie)
            .border(brutal.grosor, trazo)
            .padding(brutal.grosor + 3.dp),
        contentAlignment = if (activo) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(20.dp).background(if (activo && enabled) ColoresBrutal.Tinta else trazo))
    }
}

/** Fila con texto e interruptor: toda la fila se toca y TalkBack la anuncia como un interruptor. */
@Composable
fun FilaInterruptor(
    texto: String,
    activo: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    enabled: Boolean = true,
) {
    val brutal = LocalBrutal.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = activo, enabled = enabled, role = Role.Switch, onValueChange = onCambio),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(texto, style = MaterialTheme.typography.titleSmall, color = if (enabled) brutal.texto else brutal.textoSuave)
            ayuda?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = brutal.textoSuave) }
        }
        InterruptorBrutal(activo, enabled = enabled)
    }
}

/** Grupo de opciones excluyentes: envolver las [OpcionBrutal] en una columna con esto. */
fun Modifier.grupoOpciones(): Modifier = selectableGroup()

/** Opción de radio: círculo de trazo grueso, relleno de tinta cuando está elegida. */
@Composable
fun OpcionBrutal(texto: String, elegida: Boolean, onElegir: () -> Unit, modifier: Modifier = Modifier) {
    val brutal = LocalBrutal.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = elegida, onClick = onElegir, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(24.dp)
                .background(if (elegida) ColoresBrutal.Cian else brutal.superficie, CircleShape)
                .border(brutal.grosor, brutal.trazo, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (elegida) Box(Modifier.size(8.dp).background(ColoresBrutal.Tinta, CircleShape))
        }
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = brutal.texto)
    }
}

/** Ficha de filtro rectangular. Elegida: fondo cian. El área táctil llega a 48 dp aunque se vea más baja. */
@Composable
fun FichaFiltro(texto: String, elegida: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val brutal = LocalBrutal.current
    val interacciones = remember { MutableInteractionSource() }
    val enfocado by interacciones.collectIsFocusedAsState()
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .selectable(
                selected = elegida,
                interactionSource = interacciones,
                indication = null,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .background(if (elegida) ColoresBrutal.Cian else brutal.superficie)
            .border(2.dp, if (enfocado) brutal.foco else brutal.trazo)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            if (elegida) "✓ $texto" else texto,
            style = MaterialTheme.typography.labelLarge,
            color = if (elegida) ColoresBrutal.Tinta else brutal.texto,
        )
    }
}

/**
 * Barra de progreso con borde. Con [fraccion] nula es indeterminada: rayas diagonales que avanzan.
 */
@Composable
fun BarraProgresoBrutal(fraccion: Float?, modifier: Modifier = Modifier) {
    val brutal = LocalBrutal.current
    val semantica = if (fraccion != null) Modifier.progressSemantics(fraccion) else Modifier.progressSemantics()
    Box(
        modifier
            .then(semantica)
            .fillMaxWidth()
            .height(20.dp)
            .background(brutal.superficie)
            .border(brutal.grosor, brutal.trazo)
            .padding(brutal.grosor)
            .clipToBounds(),
    ) {
        if (fraccion != null) {
            Box(Modifier.fillMaxWidth(fraccion.coerceIn(0f, 1f)).fillMaxHeight().background(ColoresBrutal.Cian))
        } else {
            val transicion = rememberInfiniteTransition(label = "rayas")
            val desfase by transicion.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
                label = "desfase",
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(ColoresBrutal.Cian)
                    .drawBehind {
                        val paso = 16.dp.toPx()
                        val inicio = -size.height + desfase * paso
                        var x = inicio - paso
                        while (x < size.width) {
                            drawLine(
                                ColoresBrutal.Tinta,
                                start = Offset(x, size.height),
                                end = Offset(x + size.height, 0f),
                                strokeWidth = paso / 3,
                            )
                            x += paso
                        }
                    },
            )
        }
    }
}

@Preview(name = "Controles · claro", showBackground = true, backgroundColor = 0xFFF4EFE3, widthDp = 380)
@Preview(name = "Controles · oscuro", showBackground = true, backgroundColor = 0xFF141414, uiMode = 0x21, widthDp = 380)
@Composable
private fun VistaPreviaControles() {
    TemaSenasLSC {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            var texto by remember { mutableStateOf("hola") }
            CampoBrutal(texto, { texto = it }, "Buscar")
            FilaInterruptor("Guardar frases", activo = true, onCambio = {}, ayuda = "Solo en este teléfono")
            FilaInterruptor("Desactivado", activo = false, onCambio = {})
            Column(Modifier.grupoOpciones()) {
                OpcionBrutal("Automático", elegida = true, onElegir = {})
                OpcionBrutal("Solo en el teléfono", elegida = false, onElegir = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FichaFiltro("Todas", elegida = true, onClick = {})
                FichaFiltro("Letras", elegida = false, onClick = {})
            }
            BarraProgresoBrutal(0.42f)
            BarraProgresoBrutal(null)
        }
    }
}
