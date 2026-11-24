package com.lhansqw.senaslsc.acerca

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.BuildConfig
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.recognizer.ContratoModelo
import com.lhansqw.senaslsc.recognizer.ResultadoContrato
import com.lhansqw.senaslsc.recognizer.litert.CargadorReconocedor
import com.lhansqw.senaslsc.reconocimiento.resumenMetricas
import com.lhansqw.senaslsc.ui.brutal.BarraSuperiorBrutal
import com.lhansqw.senaslsc.ui.brutal.Insignia
import com.lhansqw.senaslsc.ui.brutal.Separador
import com.lhansqw.senaslsc.ui.brutal.TarjetaBrutal
import com.lhansqw.senaslsc.ui.brutal.TituloSeccion
import com.lhansqw.senaslsc.ui.theme.LocalBrutal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** «Acerca de»: qué hace la app, privacidad, modelo instalado, créditos y licencias (fase 12). */
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
        topBar = { BarraSuperiorBrutal(stringResource(R.string.acerca_titulo), onVolver) },
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
                    Text(stringResource(R.string.vocabulario_modelo, c.nombre, c.version, c.visibles.size), style = MaterialTheme.typography.titleSmall)
                    resumenMetricas(c.metricas)?.let {
                        Text(stringResource(R.string.acerca_metricas, it), style = MaterialTheme.typography.bodySmall)
                    }
                    c.datosEntrenamiento.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
            Seccion(stringResource(R.string.acerca_datos)) {
                Text(Creditos.CITA_LSC70, style = MaterialTheme.typography.bodyMedium)
                Enlace(Creditos.datos.enlace)
                Separador()
                Text(Creditos.CITA_LSC50, style = MaterialTheme.typography.bodyMedium)
                Enlace(Creditos.datosLsc50.enlace)
            }
            Seccion(stringResource(R.string.acerca_licencias)) {
                Creditos.componentes.forEachIndexed { i, comp ->
                    if (i > 0) Separador()
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${comp.nombre} · ${comp.licencia}", style = MaterialTheme.typography.titleSmall)
                        Text(comp.uso, style = MaterialTheme.typography.bodySmall)
                        Enlace(comp.enlace)
                    }
                }
            }
            Insignia(stringResource(R.string.inicio_version, BuildConfig.VERSION_NAME))
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    TarjetaBrutal {
        TituloSeccion(titulo)
        contenido()
    }
}

/** Dirección web que se abre en el navegador: negrita y subrayada, como en la referencia. */
@Composable
private fun Enlace(url: String) {
    val estilo = TextLinkStyles(
        SpanStyle(color = LocalBrutal.current.enlace, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
    )
    Text(
        buildAnnotatedString { withLink(LinkAnnotation.Url(url, estilo)) { append(url) } },
        style = MaterialTheme.typography.bodySmall,
    )
}
