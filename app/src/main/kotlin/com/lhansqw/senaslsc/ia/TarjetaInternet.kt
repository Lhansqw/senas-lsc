package com.lhansqw.senaslsc.ia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.interpreter.PreferenciaInterprete

/** Resultado de «Probar conexión». */
sealed interface EstadoPrueba {
    data object Ninguna : EstadoPrueba
    data object Probando : EstadoPrueba
    data object Correcta : EstadoPrueba
    data class Fallida(val motivo: String) : EstadoPrueba
}

/**
 * IA de internet (Gemini, fase 10): consentimiento, clave, modelo, preferencia y prueba.
 * Sin consentimiento solo se muestra la explicación y el botón para activarla.
 */
@Composable
fun TarjetaInternet(
    consentimiento: Boolean,
    hayClave: Boolean,
    modelo: String,
    preferencia: PreferenciaInterprete,
    prueba: EstadoPrueba,
    onConsentimiento: (Boolean) -> Unit,
    onGuardarClave: (String) -> Unit,
    onBorrarClave: () -> Unit,
    onModelo: (String) -> Unit,
    onPreferencia: (PreferenciaInterprete) -> Unit,
    onProbar: () -> Unit,
) {
    var pedirConsentimiento by rememberSaveable { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.remota_titulo),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.remota_explicacion), style = MaterialTheme.typography.bodyMedium)

            if (!consentimiento) {
                Button(onClick = { pedirConsentimiento = true }) { Text(stringResource(R.string.remota_activar)) }
            } else {
                AjustesInternet(hayClave, modelo, preferencia, prueba, onConsentimiento, onGuardarClave, onBorrarClave, onModelo, onPreferencia, onProbar)
            }
        }
    }

    if (pedirConsentimiento) {
        AlertDialog(
            onDismissRequest = { pedirConsentimiento = false },
            title = { Text(stringResource(R.string.remota_consentimiento_titulo)) },
            text = { Text(stringResource(R.string.remota_consentimiento_texto)) },
            confirmButton = {
                Button(onClick = {
                    pedirConsentimiento = false
                    onConsentimiento(true)
                }) { Text(stringResource(R.string.remota_acepto)) }
            },
            dismissButton = {
                TextButton(onClick = { pedirConsentimiento = false }) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}

@Composable
private fun AjustesInternet(
    hayClave: Boolean,
    modelo: String,
    preferencia: PreferenciaInterprete,
    prueba: EstadoPrueba,
    onConsentimiento: (Boolean) -> Unit,
    onGuardarClave: (String) -> Unit,
    onBorrarClave: () -> Unit,
    onModelo: (String) -> Unit,
    onPreferencia: (PreferenciaInterprete) -> Unit,
    onProbar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.remota_permitida), modifier = Modifier.weight(1f))
            Switch(checked = true, onCheckedChange = { if (!it) onConsentimiento(false) })
        }

        if (hayClave) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.remota_clave_guardada), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onBorrarClave) { Text(stringResource(R.string.remota_borrar_clave)) }
            }
        } else {
            var clave by rememberSaveable { mutableStateOf("") }
            OutlinedTextField(
                value = clave,
                onValueChange = { clave = it },
                label = { Text(stringResource(R.string.remota_clave)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.remota_clave_ayuda), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { onGuardarClave(clave); clave = "" }, enabled = clave.isNotBlank()) {
                Text(stringResource(R.string.remota_guardar_clave))
            }
        }

        var modeloEditado by rememberSaveable(modelo) { mutableStateOf(modelo) }
        OutlinedTextField(
            value = modeloEditado,
            onValueChange = { modeloEditado = it },
            label = { Text(stringResource(R.string.remota_modelo)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (modeloEditado.trim() != modelo) {
            TextButton(onClick = { onModelo(modeloEditado) }) { Text(stringResource(R.string.remota_usar_modelo)) }
        }

        Text(stringResource(R.string.remota_cuando), style = MaterialTheme.typography.titleSmall)
        Column(Modifier.selectableGroup()) {
            OpcionPreferencia(PreferenciaInterprete.AUTOMATICO, R.string.remota_pref_auto, preferencia, onPreferencia)
            OpcionPreferencia(PreferenciaInterprete.PREFERIR_INTERNET, R.string.remota_pref_internet, preferencia, onPreferencia)
            OpcionPreferencia(PreferenciaInterprete.SOLO_TELEFONO, R.string.remota_pref_telefono, preferencia, onPreferencia)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onProbar, enabled = hayClave && prueba != EstadoPrueba.Probando) {
                Text(stringResource(R.string.remota_probar))
            }
            when (prueba) {
                EstadoPrueba.Ninguna -> Unit
                EstadoPrueba.Probando -> Text(stringResource(R.string.remota_probando))
                EstadoPrueba.Correcta -> Text(stringResource(R.string.remota_prueba_ok), color = MaterialTheme.colorScheme.primary)
                is EstadoPrueba.Fallida -> Text(prueba.motivo, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun OpcionPreferencia(
    valor: PreferenciaInterprete,
    texto: Int,
    actual: PreferenciaInterprete,
    onElegir: (PreferenciaInterprete) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = valor == actual, onClick = { onElegir(valor) }, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = valor == actual, onClick = null)
        Text(stringResource(texto), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}
