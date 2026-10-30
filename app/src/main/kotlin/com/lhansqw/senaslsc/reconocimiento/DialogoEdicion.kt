package com.lhansqw.senaslsc.reconocimiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.data.history.EntradaHistorial

/** Diálogo para corregir la frase propuesta. El texto se conserva al girar el teléfono. */
@Composable
fun DialogoEdicion(
    textoInicial: String,
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit,
) {
    var texto by rememberSaveable { mutableStateOf(textoInicial) }
    val valido = texto.isNotBlank() && texto.length <= EntradaHistorial.LONGITUD_MAXIMA

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.editar_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text(stringResource(R.string.editar_etiqueta)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.editar_ayuda),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmar(texto) }, enabled = valido) { Text(stringResource(R.string.accion_guardar)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) }
        },
    )
}
