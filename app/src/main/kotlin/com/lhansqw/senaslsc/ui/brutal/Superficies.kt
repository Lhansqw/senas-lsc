package com.lhansqw.senaslsc.ui.brutal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.ui.theme.ColoresBrutal
import com.lhansqw.senaslsc.ui.theme.LocalBrutal
import com.lhansqw.senaslsc.ui.theme.TemaSenasLSC

/**
 * Tarjeta brutalista: bloque con borde y sombra.
 *
 * @param color fondo de acento (cian, magenta, amarillo); el texto pasa a tinta en los dos modos.
 */
@Composable
fun TarjetaBrutal(
    modifier: Modifier = Modifier,
    color: Color? = null,
    espacio: Dp = 8.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val brutal = LocalBrutal.current
    CompositionLocalProvider(LocalContentColor provides if (color != null) ColoresBrutal.Tinta else brutal.texto) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .bloqueBrutal(color ?: brutal.superficie, brutal.trazo, brutal.grosor, brutal.sombra)
                .padding(brutal.grosor + 14.dp),
            verticalArrangement = Arrangement.spacedBy(espacio),
            content = contenido,
        )
    }
}

/** Título de una tarjeta o sección, marcado como encabezado para TalkBack. */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = modifier.semantics { heading() })
}

enum class TipoAviso { Info, Aviso, Error }

/** Banda de aviso: color plano según el tipo y un signo a la izquierda que TalkBack no lee. */
@Composable
fun AvisoBrutal(texto: String, modifier: Modifier = Modifier, titulo: String? = null, tipo: TipoAviso = TipoAviso.Aviso) {
    val brutal = LocalBrutal.current
    val (fondo, tinta) = when (tipo) {
        TipoAviso.Info -> ColoresBrutal.Cian to ColoresBrutal.Tinta
        TipoAviso.Aviso -> ColoresBrutal.Amarillo to ColoresBrutal.Tinta
        TipoAviso.Error -> ColoresBrutal.Rojo to ColoresBrutal.Blanco
    }
    CompositionLocalProvider(LocalContentColor provides tinta) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .bloqueBrutal(fondo, brutal.trazo, brutal.grosor, brutal.sombra)
                .padding(brutal.grosor + 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(tinta)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text(if (tipo == TipoAviso.Info) "i" else "!", color = fondo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                titulo?.let { TituloSeccion(it) }
                Text(texto, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Barra superior: botón volver cuadrado, título en Archivo y una línea gruesa debajo.
 * Respeta la barra de estado y los recortes de pantalla.
 */
@Composable
fun BarraSuperiorBrutal(titulo: String, onVolver: (() -> Unit)? = null) {
    val brutal = LocalBrutal.current
    Column(
        Modifier
            .background(brutal.fondo)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            onVolver?.let {
                BotonBrutal(onClick = it, variante = VarianteBoton.Secundario) {
                    Icon(
                        painterResource(R.drawable.ic_volver),
                        contentDescription = stringResource(R.string.accion_volver),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                titulo,
                style = MaterialTheme.typography.titleLarge,
                color = brutal.texto,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
        Separador(grosor = brutal.grosor)
    }
}

@Preview(name = "Superficies · claro", showBackground = true, backgroundColor = 0xFFF4EFE3, widthDp = 380)
@Preview(name = "Superficies · oscuro", showBackground = true, backgroundColor = 0xFF141414, uiMode = 0x21, widthDp = 380)
@Composable
private fun VistaPreviaSuperficies() {
    TemaSenasLSC {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BarraSuperiorBrutal("Historial", onVolver = {})
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaBrutal {
                    TituloSeccion("Tu dispositivo")
                    Text("Android API 34 · 4 GB de RAM")
                    Insignia("Disponible", color = ColoresBrutal.Cian)
                }
                TarjetaBrutal(color = ColoresBrutal.Magenta) { TituloSeccion("Tarjeta de acento") }
                AvisoBrutal("La app propone frases; revísalas antes de usarlas.", titulo = "Limitaciones")
                AvisoBrutal("No se pudo leer el historial anterior.", tipo = TipoAviso.Error)
                AvisoBrutal("Se descarga una sola vez.", tipo = TipoAviso.Info)
            }
        }
    }
}
