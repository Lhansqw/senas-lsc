package com.lhansqw.senaslsc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.BuildConfig
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.dispositivo.CapacidadDispositivo
import com.lhansqw.senaslsc.dispositivo.EvaluadorCapacidad
import com.lhansqw.senaslsc.dispositivo.PerfilDispositivo
import com.lhansqw.senaslsc.ui.theme.TemaSenasLSC

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicio(
    perfil: PerfilDispositivo,
    capacidad: CapacidadDispositivo,
    onAbrirCamara: () -> Unit,
    onAbrirIa: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { relleno ->
        Column(
            modifier = Modifier
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.inicio_titulo),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.inicio_descripcion),
                style = MaterialTheme.typography.bodyLarge,
            )

            Button(
                onClick = onAbrirCamara,
                enabled = capacidad.reconocedorLocal,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.inicio_abrir_camara)) }
            OutlinedButton(
                onClick = onAbrirIa,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.inicio_abrir_ia)) }

            TarjetaDispositivo(perfil, capacidad)
            TarjetaLimitaciones()

            Text(
                text = stringResource(R.string.inicio_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TarjetaDispositivo(perfil: PerfilDispositivo, capacidad: CapacidadDispositivo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.dispositivo_titulo),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Fila(stringResource(R.string.dispositivo_android), "API ${perfil.nivelApi}")
            Fila(stringResource(R.string.dispositivo_ram), EvaluadorCapacidad.formatearGb(perfil.ramTotalMb))
            Fila(stringResource(R.string.dispositivo_nucleos), perfil.nucleos.toString())
            Fila(
                stringResource(R.string.dispositivo_reconocedor),
                estado(capacidad.reconocedorLocal),
            )
            Fila(
                stringResource(R.string.dispositivo_interprete),
                if (capacidad.interpreteLocal) {
                    stringResource(R.string.estado_disponible)
                } else {
                    stringResource(R.string.estado_solo_reglas)
                },
            )
            capacidad.motivos.forEach { motivo ->
                Text(
                    text = motivo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TarjetaLimitaciones() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.limitaciones_titulo),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.limitaciones_texto),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun Fila(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun estado(disponible: Boolean): String =
    stringResource(if (disponible) R.string.estado_disponible else R.string.estado_no_disponible)

@Preview(showBackground = true, name = "Gama media")
@Composable
private fun VistaPreviaInicio() {
    val perfil = PerfilDispositivo(nivelApi = 34, ramTotalMb = 3_800, esDispositivoBajaMemoria = false, nucleos = 8)
    TemaSenasLSC(colorDinamico = false) {
        PantallaInicio(perfil, EvaluadorCapacidad.evaluar(perfil), onAbrirCamara = {}, onAbrirIa = {})
    }
}
