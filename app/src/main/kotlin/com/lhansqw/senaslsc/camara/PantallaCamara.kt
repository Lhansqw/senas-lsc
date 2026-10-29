package com.lhansqw.senaslsc.camara

import androidx.activity.compose.BackHandler
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lhansqw.senaslsc.BuildConfig
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.camera.EstadoCamara
import com.lhansqw.senaslsc.camera.Rendimiento
import com.lhansqw.senaslsc.permisos.ConPermisoCamara
import com.lhansqw.senaslsc.ui.theme.TemaSenasLSC
import java.util.Locale

private val CARGAS_SIMULADAS_MS = listOf(0L, 50L, 150L)

@Composable
fun PantallaCamara(onVolver: () -> Unit) {
    BackHandler(onBack = onVolver)
    ConPermisoCamara(onVolver = onVolver) {
        // Tema oscuro fijo: los controles se dibujan sobre la imagen de la cámara.
        TemaSenasLSC(oscuro = true, colorDinamico = false) {
            VistaCamara(onVolver = onVolver)
        }
    }
}

@Composable
private fun VistaCamara(onVolver: () -> Unit, viewModel: CamaraViewModel = viewModel()) {
    val controlador = viewModel.controlador
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // La cámara se vincula mientras esta pantalla está en composición y se libera al salir.
    LaunchedEffect(lifecycleOwner) {
        controlador.vincular(context, lifecycleOwner)
    }
    MantenerPantallaEncendida()

    val solicitud by controlador.surfaceRequest.collectAsStateWithLifecycle()
    val estado by controlador.estado.collectAsStateWithLifecycle()
    val rendimiento by controlador.rendimiento.collectAsStateWithLifecycle()
    val analisisActivo by controlador.analisisActivo.collectAsStateWithLifecycle()
    val cargaSimulada by viewModel.cargaSimuladaMs.collectAsStateWithLifecycle()
    val descripcionVista = stringResource(R.string.camara_vista_previa)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        solicitud?.let { request ->
            CameraXViewfinder(
                surfaceRequest = request,
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = descripcionVista },
            )
        }

        when (val e = estado) {
            EstadoCamara.Iniciando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            EstadoCamara.SinCamara -> MensajeCentral(stringResource(R.string.camara_sin_camara))
            is EstadoCamara.Error -> MensajeCentral(e.mensaje)
            is EstadoCamara.Activa -> Unit
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            PanelMetricas(estado, rendimiento, analisisActivo)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (BuildConfig.DEBUG) {
                    SelectorCargaSimulada(cargaSimulada, viewModel::establecerCargaSimulada)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onVolver,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.accion_volver)) }
                    Button(
                        onClick = viewModel::alternarAnalisis,
                        enabled = estado is EstadoCamara.Activa,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(if (analisisActivo) R.string.camara_pausar else R.string.camara_reanudar))
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelMetricas(estado: EstadoCamara, rendimiento: Rendimiento, analisisActivo: Boolean) {
    val activa = estado as? EstadoCamara.Activa
    val es = Locale.forLanguageTag("es-CO")
    val lineas = buildList {
        add(
            if (analisisActivo) {
                stringResource(R.string.camara_fps, String.format(es, "%.1f", rendimiento.fps))
            } else {
                stringResource(R.string.camara_analisis_pausado)
            },
        )
        if (analisisActivo) {
            add(
                stringResource(
                    R.string.camara_latencia,
                    String.format(es, "%.1f", rendimiento.latenciaMediaMs),
                    String.format(es, "%.1f", rendimiento.latenciaMaximaMs),
                ),
            )
        }
        activa?.resolucionAnalisis?.let { add(stringResource(R.string.camara_resolucion, it.width, it.height)) }
        activa?.let {
            add(stringResource(if (it.frontal) R.string.camara_frontal else R.string.camara_trasera))
        }
    }
    Column(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        lineas.forEach { linea ->
            Text(linea, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun SelectorCargaSimulada(actual: Long, onCambiar: (Long) -> Unit) {
    Column(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(stringResource(R.string.camara_carga_simulada), color = Color.White, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CARGAS_SIMULADAS_MS.forEach { ms ->
                FilterChip(
                    selected = actual == ms,
                    onClick = { onCambiar(ms) },
                    label = { Text("$ms ms") },
                )
            }
        }
    }
}

@Composable
private fun MensajeCentral(texto: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(texto, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Evita que la pantalla se apague mientras la persona hace señas. */
@Composable
private fun MantenerPantallaEncendida() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
