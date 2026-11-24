package com.lhansqw.senaslsc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.BuildConfig
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.dispositivo.CapacidadDispositivo
import com.lhansqw.senaslsc.dispositivo.EvaluadorCapacidad
import com.lhansqw.senaslsc.dispositivo.PerfilDispositivo
import com.lhansqw.senaslsc.ui.brutal.AvisoBrutal
import com.lhansqw.senaslsc.ui.brutal.BotonBrutal
import com.lhansqw.senaslsc.ui.brutal.Insignia
import com.lhansqw.senaslsc.ui.brutal.Separador
import com.lhansqw.senaslsc.ui.brutal.TarjetaBrutal
import com.lhansqw.senaslsc.ui.brutal.TipoAviso
import com.lhansqw.senaslsc.ui.brutal.TituloSeccion
import com.lhansqw.senaslsc.ui.brutal.VarianteBoton
import com.lhansqw.senaslsc.ui.theme.ColoresBrutal
import com.lhansqw.senaslsc.ui.theme.LocalBrutal
import com.lhansqw.senaslsc.ui.theme.TemaSenasLSC

@Composable
fun PantallaInicio(
    perfil: PerfilDispositivo,
    capacidad: CapacidadDispositivo,
    onAbrirCamara: () -> Unit,
    onAbrirIa: () -> Unit,
    onAbrirHistorial: () -> Unit,
    onAbrirVocabulario: () -> Unit,
    onAbrirAcerca: () -> Unit,
    onAbrirGrabacion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brutal = LocalBrutal.current
    Scaffold(modifier = modifier.fillMaxSize()) { relleno ->
        Column(
            modifier = Modifier
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(text = stringResource(R.string.inicio_titulo), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.inicio_descripcion),
                    style = MaterialTheme.typography.bodyLarge,
                    color = brutal.textoSuave,
                )
            }

            BotonBrutal(
                onClick = onAbrirCamara,
                enabled = capacidad.reconocedorLocal,
                modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
            ) {
                Text(stringResource(R.string.inicio_abrir_camara), style = MaterialTheme.typography.headlineSmall)
            }
            // Sin reconocedor la cámara no sirve: se dice por qué en vez de dejar el botón apagado sin más.
            if (!capacidad.reconocedorLocal && capacidad.motivos.isNotEmpty()) {
                AvisoBrutal(capacidad.motivos.joinToString("\n"), tipo = TipoAviso.Error)
            }

            Mosaicos(
                Mosaico(R.string.inicio_abrir_ia, ColoresBrutal.Magenta, onAbrirIa),
                Mosaico(R.string.inicio_abrir_vocabulario, ColoresBrutal.Amarillo, onAbrirVocabulario),
            )
            Mosaicos(
                Mosaico(R.string.inicio_abrir_historial, null, onAbrirHistorial),
                Mosaico(R.string.inicio_abrir_grabacion, null, onAbrirGrabacion, enabled = capacidad.reconocedorLocal),
            )
            BotonBrutal(
                stringResource(R.string.inicio_abrir_acerca),
                onClick = onAbrirAcerca,
                variante = VarianteBoton.Texto,
                modifier = Modifier.fillMaxWidth(),
            )

            TarjetaDispositivo(perfil, capacidad)
            AvisoBrutal(stringResource(R.string.limitaciones_texto), titulo = stringResource(R.string.limitaciones_titulo))

            Insignia(stringResource(R.string.inicio_version, BuildConfig.VERSION_NAME))
        }
    }
}

/** Un acceso de la cuadrícula de inicio. Sin color usa el de la superficie. */
private class Mosaico(val texto: Int, val color: Color?, val onClick: () -> Unit, val enabled: Boolean = true)

/** Dos mosaicos lado a lado, con la misma altura aunque uno tenga más texto. */
@Composable
private fun Mosaicos(izquierdo: Mosaico, derecho: Mosaico) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf(izquierdo, derecho).forEach { m ->
            BotonBrutal(
                onClick = m.onClick,
                enabled = m.enabled,
                variante = VarianteBoton.Secundario,
                color = m.color,
                modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 96.dp),
            ) {
                Text(stringResource(m.texto), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun TarjetaDispositivo(perfil: PerfilDispositivo, capacidad: CapacidadDispositivo) {
    TarjetaBrutal(espacio = 10.dp) {
        TituloSeccion(stringResource(R.string.dispositivo_titulo))
        Separador()
        Fila(stringResource(R.string.dispositivo_android)) { Valor("API ${perfil.nivelApi}") }
        Separador()
        Fila(stringResource(R.string.dispositivo_ram)) { Valor(EvaluadorCapacidad.formatearGb(perfil.ramTotalMb)) }
        Separador()
        Fila(stringResource(R.string.dispositivo_nucleos)) { Valor(perfil.nucleos.toString()) }
        Separador()
        Fila(stringResource(R.string.dispositivo_reconocedor)) {
            if (capacidad.reconocedorLocal) {
                Insignia(stringResource(R.string.estado_disponible), color = ColoresBrutal.Cian)
            } else {
                Insignia(stringResource(R.string.estado_no_disponible))
            }
        }
        Separador()
        Fila(stringResource(R.string.dispositivo_interprete)) {
            if (capacidad.interpreteLocal) {
                Insignia(stringResource(R.string.estado_disponible), color = ColoresBrutal.Cian)
            } else {
                Insignia(stringResource(R.string.estado_solo_reglas), color = ColoresBrutal.Amarillo)
            }
        }
        // Si falta el reconocedor, los motivos ya están en el aviso de arriba.
        if (capacidad.reconocedorLocal) {
            capacidad.motivos.forEach { motivo ->
                Text(text = motivo, style = MaterialTheme.typography.bodyMedium, color = LocalBrutal.current.textoSuave)
            }
        }
    }
}

@Composable
private fun Fila(etiqueta: String, valor: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        valor()
    }
}

@Composable
private fun Valor(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleSmall)
}

@Preview(showBackground = true, name = "Gama media · claro", heightDp = 1400)
@Preview(showBackground = true, name = "Gama media · oscuro", heightDp = 1400, uiMode = 0x21)
@Composable
private fun VistaPreviaInicio() {
    val perfil = PerfilDispositivo(nivelApi = 34, ramTotalMb = 3_800, esDispositivoBajaMemoria = false, nucleos = 8)
    TemaSenasLSC {
        PantallaInicio(perfil, EvaluadorCapacidad.evaluar(perfil), onAbrirCamara = {}, onAbrirIa = {}, onAbrirHistorial = {}, onAbrirVocabulario = {}, onAbrirAcerca = {}, onAbrirGrabacion = {})
    }
}

@Preview(showBackground = true, name = "Gama baja", heightDp = 1400)
@Composable
private fun VistaPreviaInicioGamaBaja() {
    val perfil = PerfilDispositivo(nivelApi = 26, ramTotalMb = 1_500, esDispositivoBajaMemoria = true, nucleos = 4)
    TemaSenasLSC {
        PantallaInicio(perfil, EvaluadorCapacidad.evaluar(perfil), onAbrirCamara = {}, onAbrirIa = {}, onAbrirHistorial = {}, onAbrirVocabulario = {}, onAbrirAcerca = {}, onAbrirGrabacion = {})
    }
}
