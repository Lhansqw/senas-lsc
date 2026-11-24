package com.lhansqw.senaslsc.ui.brutal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.ui.theme.LocalBrutal
import com.lhansqw.senaslsc.ui.theme.TemaSenasLSC

/**
 * Diálogo de confirmación: título en Archivo, texto y, abajo a la derecha, «Cancelar» y la acción.
 *
 * @param peligro la acción borra algo: el botón va en rojo.
 * @param otrasAcciones botones extra que van antes de «Cancelar» (por ejemplo, «Ver licencia»).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DialogoBrutal(
    titulo: String,
    texto: String,
    accion: String,
    onAccion: () -> Unit,
    onCerrar: () -> Unit,
    peligro: Boolean = false,
    otrasAcciones: @Composable () -> Unit = {},
) {
    val brutal = LocalBrutal.current
    BasicAlertDialog(onDismissRequest = onCerrar) {
        Column(
            Modifier
                .bloqueBrutal(brutal.superficie, brutal.trazo, brutal.grosor, brutal.sombra * 2)
                .padding(brutal.grosor + 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                titulo,
                style = MaterialTheme.typography.headlineSmall,
                color = brutal.texto,
                modifier = Modifier.semantics { heading() },
            )
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = brutal.texto)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                otrasAcciones()
                BotonBrutal(stringResource(R.string.accion_cancelar), onCerrar, variante = VarianteBoton.Texto)
                BotonBrutal(accion, onAccion, variante = if (peligro) VarianteBoton.Peligro else VarianteBoton.Primario)
            }
        }
    }
}

@Preview(name = "Diálogo · claro", showBackground = true, backgroundColor = 0xFFF4EFE3, widthDp = 380, heightDp = 400)
@Composable
private fun VistaPreviaDialogo() {
    TemaSenasLSC {
        DialogoBrutal(
            titulo = "¿Vaciar el historial?",
            texto = "Se borran todas las frases guardadas en este teléfono.",
            accion = "Vaciar",
            onAccion = {},
            onCerrar = {},
            peligro = true,
        )
    }
}
