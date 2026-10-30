package com.lhansqw.senaslsc.acerca

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.BuildConfig
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.recognizer.ContratoModelo
import com.lhansqw.senaslsc.recognizer.ResultadoContrato
import com.lhansqw.senaslsc.recognizer.litert.CargadorReconocedor
import com.lhansqw.senaslsc.reconocimiento.resumenMetricas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** «Acerca de»: qué hace la app, privacidad, modelo instalado, créditos y licencias (fase 12). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAcerca(onVolver: () -> Unit) {
    BackHandler(onBack = onVolver)
    val context = LocalContext.current
    // Solo se lee modelo.json (no se carga el modelo): es rápido, pero es E/S.
    val contrato by produceState<ContratoModelo?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            (CargadorReconocedor.leerContrato(context) as? ResultadoContrato.Valido)?.contrato
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.acerca_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(painterResource(R.drawable.ic_volver), contentDescription = stringResource(R.string.accion_volver))
                    }
                },
            )
        },
    ) { relleno ->
        Column(
            modifier = Modifier
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Seccion(stringResource(R.string.acerca_que_hace)) {
                Text(stringResource(R.string.limitaciones_texto), style = MaterialTheme.typography.bodyMedium)
            }
            Seccion(stringResource(R.string.acerca_privacidad)) {
                Text(stringResource(R.string.acerca_privacidad_texto), style = MaterialTheme.typography.bodyMedium)
            }
            Seccion(stringResource(R.string.acerca_modelo)) {
                val c = contrato
                if (c == null) {
                    Text(stringResource(R.string.vocabulario_sin_modelo), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(stringResource(R.string.vocabulario_modelo, c.nombre, c.version, c.entradas.size), fontWeight = FontWeight.SemiBold)
                    resumenMetricas(c.metricas)?.let {
                        Text(stringResource(R.string.acerca_metricas, it), style = MaterialTheme.typography.bodySmall)
                    }
                    c.datosEntrenamiento.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
            Seccion(stringResource(R.string.acerca_datos)) {
                Text(Creditos.CITA_LSC70, style = MaterialTheme.typography.bodyMedium)
                Text(Creditos.datos.enlace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Seccion(stringResource(R.string.acerca_licencias)) {
                Creditos.componentes.forEachIndexed { i, comp ->
                    if (i > 0) HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${comp.nombre} · ${comp.licencia}", fontWeight = FontWeight.SemiBold)
                        Text(comp.uso, style = MaterialTheme.typography.bodySmall)
                        Text(comp.enlace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Text(
                stringResource(R.string.inicio_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            contenido()
        }
    }
}
