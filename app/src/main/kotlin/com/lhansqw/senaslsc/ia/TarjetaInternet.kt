package com.lhansqw.senaslsc.ia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.interpreter.PreferenciaInterprete
import com.lhansqw.senaslsc.ui.brutal.AvisoBrutal
import com.lhansqw.senaslsc.ui.brutal.BotonBrutal
import com.lhansqw.senaslsc.ui.brutal.CampoBrutal
import com.lhansqw.senaslsc.ui.brutal.DialogoBrutal
import com.lhansqw.senaslsc.ui.brutal.FilaInterruptor
import com.lhansqw.senaslsc.ui.brutal.OpcionBrutal
import com.lhansqw.senaslsc.ui.brutal.Separador
import com.lhansqw.senaslsc.ui.brutal.TarjetaBrutal
import com.lhansqw.senaslsc.ui.brutal.TipoAviso
import com.lhansqw.senaslsc.ui.brutal.TituloSeccion
import com.lhansqw.senaslsc.ui.brutal.VarianteBoton
import com.lhansqw.senaslsc.ui.brutal.grupoOpciones
import com.lhansqw.senaslsc.ui.theme.ColoresBrutal
import com.lhansqw.senaslsc.ui.theme.LocalBrutal

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

    TarjetaBrutal(espacio = 12.dp) {
        TituloSeccion(stringResource(R.string.remota_titulo))
        Text(stringResource(R.string.remota_explicacion), style = MaterialTheme.typography.bodyMedium)

        if (!consentimiento) {
            BotonBrutal(stringResource(R.string.remota_activar), onClick = { pedirConsentimiento = true }, color = ColoresBrutal.Magenta)
        } else {
            AjustesInternet(hayClave, modelo, preferencia, prueba, onConsentimiento, onGuardarClave, onBorrarClave, onModelo, onPreferencia, onProbar)
        }
    }

    if (pedirConsentimiento) {
        DialogoBrutal(
            titulo = stringResource(R.string.remota_consentimiento_titulo),
            texto = stringResource(R.string.remota_consentimiento_texto),
            accion = stringResource(R.string.remota_acepto),
            onAccion = {
                pedirConsentimiento = false
                onConsentimiento(true)
            },
            onCerrar = { pedirConsentimiento = false },
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
    val brutal = LocalBrutal.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FilaInterruptor(
            texto = stringResource(R.string.remota_permitida),
            activo = true,
            onCambio = { if (!it) onConsentimiento(false) },
        )
        Separador()

        if (hayClave) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.remota_clave_guardada), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                BotonBrutal(stringResource(R.string.remota_borrar_clave), onBorrarClave, variante = VarianteBoton.Texto)
            }
        } else {
            var clave by rememberSaveable { mutableStateOf("") }
            CampoBrutal(
                valor = clave,
                onCambio = { clave = it },
                etiqueta = stringResource(R.string.remota_clave),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            Text(stringResource(R.string.remota_clave_ayuda), style = MaterialTheme.typography.bodySmall, color = brutal.textoSuave)
            BotonBrutal(
                stringResource(R.string.remota_guardar_clave),
                onClick = { onGuardarClave(clave); clave = "" },
                enabled = clave.isNotBlank(),
            )
        }

        var modeloEditado by rememberSaveable(modelo) { mutableStateOf(modelo) }
        CampoBrutal(valor = modeloEditado, onCambio = { modeloEditado = it }, etiqueta = stringResource(R.string.remota_modelo))
        if (modeloEditado.trim() != modelo) {
            BotonBrutal(stringResource(R.string.remota_usar_modelo), onClick = { onModelo(modeloEditado) }, variante = VarianteBoton.Secundario)
        }

        Separador()
        Text(stringResource(R.string.remota_cuando), style = MaterialTheme.typography.titleSmall)
        Column(Modifier.grupoOpciones()) {
            OpcionPreferencia(PreferenciaInterprete.AUTOMATICO, R.string.remota_pref_auto, preferencia, onPreferencia)
            OpcionPreferencia(PreferenciaInterprete.PREFERIR_INTERNET, R.string.remota_pref_internet, preferencia, onPreferencia)
            OpcionPreferencia(PreferenciaInterprete.SOLO_TELEFONO, R.string.remota_pref_telefono, preferencia, onPreferencia)
        }

        Separador()
        BotonBrutal(
            stringResource(R.string.remota_probar),
            onClick = onProbar,
            variante = VarianteBoton.Secundario,
            enabled = hayClave && prueba != EstadoPrueba.Probando,
        )
        when (prueba) {
            EstadoPrueba.Ninguna -> Unit
            EstadoPrueba.Probando -> Text(stringResource(R.string.remota_probando))
            EstadoPrueba.Correcta -> AvisoBrutal(stringResource(R.string.remota_prueba_ok), tipo = TipoAviso.Info)
            is EstadoPrueba.Fallida -> AvisoBrutal(prueba.motivo, tipo = TipoAviso.Error)
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
    OpcionBrutal(stringResource(texto), elegida = valor == actual, onElegir = { onElegir(valor) })
}
