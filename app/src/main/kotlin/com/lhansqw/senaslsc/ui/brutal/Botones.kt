package com.lhansqw.senaslsc.ui.brutal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.ui.theme.ColoresBrutal
import com.lhansqw.senaslsc.ui.theme.LocalBrutal
import com.lhansqw.senaslsc.ui.theme.TemaSenasLSC

enum class VarianteBoton {
    /** La acción principal de la pantalla: bloque cian. */
    Primario,
    /** Bloque del color de la superficie. */
    Secundario,
    /** Solo texto subrayado, sin bloque. */
    Texto,
    /** Acción destructiva: bloque rojo con texto blanco. */
    Peligro,
}

/**
 * Botón brutalista. Al presionarlo baja hasta tapar su sombra; el cambio es instantáneo, sin animación.
 * Deshabilitado pierde la sombra y el color y queda en trazo gris.
 *
 * @param color reemplaza el fondo del bloque (mosaicos de color); el texto va en tinta.
 */
@Composable
fun BotonBrutal(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variante: VarianteBoton = VarianteBoton.Primario,
    color: Color? = null,
    enabled: Boolean = true,
    contenido: @Composable RowScope.() -> Unit,
) {
    val brutal = LocalBrutal.current
    val interacciones = remember { MutableInteractionSource() }
    val presionado by interacciones.collectIsPressedAsState()
    val enfocado by interacciones.collectIsFocusedAsState()

    val (fondo, texto) = when {
        !enabled -> brutal.fondo to brutal.textoSuave
        color != null -> color to ColoresBrutal.Tinta
        variante == VarianteBoton.Primario -> ColoresBrutal.Cian to ColoresBrutal.Tinta
        variante == VarianteBoton.Peligro -> ColoresBrutal.Rojo to ColoresBrutal.Blanco
        variante == VarianteBoton.Texto -> Color.Transparent to brutal.texto
        else -> brutal.superficie to brutal.texto
    }

    val clic = Modifier.clickable(
        interactionSource = interacciones,
        indication = null,
        enabled = enabled,
        role = Role.Button,
        onClick = onClick,
    )
    val forma = if (variante == VarianteBoton.Texto) {
        modifier
            .then(if (enfocado) Modifier.bloqueBrutal(Color.Transparent, brutal.foco, brutal.grosor, 0.dp) else Modifier)
            .then(clic)
    } else {
        modifier
            .bloqueBrutal(
                fondo = fondo,
                trazo = if (enabled) brutal.trazo else brutal.textoSuave,
                grosor = brutal.grosor,
                sombra = brutal.sombra,
                hundido = presionado,
                foco = if (enfocado) brutal.foco else null,
                conSombra = enabled,
            )
            .then(clic)
    }

    CompositionLocalProvider(
        LocalContentColor provides texto,
        LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(
            textDecoration = if (variante == VarianteBoton.Texto) TextDecoration.Underline else null,
        ),
    ) {
        Row(
            modifier = forma
                .heightIn(min = 48.dp)
                // Al hundirse, el contenido baja con el bloque.
                .padding(
                    start = if (presionado && variante != VarianteBoton.Texto) brutal.sombra + 18.dp else 18.dp,
                    top = if (presionado && variante != VarianteBoton.Texto) brutal.sombra + 12.dp else 12.dp,
                    end = if (presionado && variante != VarianteBoton.Texto) 18.dp - brutal.sombra else 18.dp,
                    bottom = if (presionado && variante != VarianteBoton.Texto) 12.dp - brutal.sombra else 12.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = contenido,
        )
    }
}

/** Botón con una sola línea de texto. */
@Composable
fun BotonBrutal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variante: VarianteBoton = VarianteBoton.Primario,
    color: Color? = null,
    enabled: Boolean = true,
) {
    BotonBrutal(onClick, modifier, variante, color, enabled) {
        Text(texto, textAlign = TextAlign.Center)
    }
}

@Preview(name = "Botones · claro", showBackground = true, backgroundColor = 0xFFF4EFE3)
@Preview(name = "Botones · oscuro", showBackground = true, backgroundColor = 0xFF141414, uiMode = 0x21)
@Composable
private fun VistaPreviaBotones() {
    TemaSenasLSC {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonBrutal("Primario", {})
            BotonBrutal("Secundario", {}, variante = VarianteBoton.Secundario)
            BotonBrutal("Magenta", {}, color = ColoresBrutal.Magenta)
            BotonBrutal("Eliminar", {}, variante = VarianteBoton.Peligro)
            BotonBrutal("Texto", {}, variante = VarianteBoton.Texto)
            BotonBrutal("Deshabilitado", {}, enabled = false)
        }
    }
}
