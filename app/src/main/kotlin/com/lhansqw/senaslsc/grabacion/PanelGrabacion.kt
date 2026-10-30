package com.lhansqw.senaslsc.grabacion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.core.grabacion.EstadoGrabacion

/** Panel de la herramienta de grabación, sobre la vista de la cámara (fase 6). */
@Composable
fun PanelGrabacion(estado: EstadoGrabador, carpeta: String, controlador: ControladorGrabacion) {
    var alias by rememberSaveable { mutableStateOf("") }
    var senaElegida by rememberSaveable { mutableStateOf<String?>(null) }
    var elegir by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(alias) { controlador.establecerAlias(alias) }

    val activa = estado.grabacion is EstadoGrabacion.CuentaAtras || estado.grabacion is EstadoGrabacion.Grabando
    val etiqueta = estado.etiquetas.firstOrNull { it.id == senaElegida }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.grabacion_titulo), color = Color.White, fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = alias,
            onValueChange = { alias = it.lowercase().take(24) },
            label = { Text(stringResource(R.string.grabacion_alias)) },
            singleLine = true,
            enabled = !activa,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { elegir = true }, enabled = !activa && estado.etiquetas.isNotEmpty()) {
                Text(etiqueta?.glosa ?: stringResource(R.string.grabacion_elegir_sena))
            }
            etiqueta?.let {
                Text(
                    stringResource(R.string.grabacion_guardadas, estado.guardadas[it.id] ?: 0),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        val textoEstado = when (val g = estado.grabacion) {
            EstadoGrabacion.Inactivo -> null
            is EstadoGrabacion.CuentaAtras -> stringResource(R.string.grabacion_preparate, ((g.restanteMs + 999) / 1000).toInt())
            is EstadoGrabacion.Grabando -> stringResource(R.string.grabacion_grabando)
            is EstadoGrabacion.Terminada -> stringResource(R.string.grabacion_guardada)
            is EstadoGrabacion.Fallida -> g.motivo
        }
        (estado.mensaje ?: textoEstado)?.let {
            Text(
                it,
                color = if (estado.grabacion is EstadoGrabacion.Grabando) Color(0xFFFF8A80) else Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        if (activa) {
            OutlinedButton(onClick = controlador::cancelar) { Text(stringResource(R.string.accion_cancelar)) }
        } else {
            Button(onClick = { senaElegida?.let(controlador::grabar) }, enabled = senaElegida != null && alias.isNotBlank()) {
                Text(stringResource(R.string.grabacion_grabar))
            }
        }

        Text(stringResource(R.string.grabacion_ayuda, carpeta), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
    }

    if (elegir) {
        AlertDialog(
            onDismissRequest = { elegir = false },
            title = { Text(stringResource(R.string.grabacion_elegir_sena)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(estado.etiquetas, key = { it.id }) { e ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    senaElegida = e.id
                                    elegir = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(e.glosa)
                            Text("${estado.guardadas[e.id] ?: 0}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { elegir = false }) { Text(stringResource(R.string.accion_cancelar)) } },
        )
    }
}
