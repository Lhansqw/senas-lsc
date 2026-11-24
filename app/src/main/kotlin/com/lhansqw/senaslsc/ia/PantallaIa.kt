package com.lhansqw.senaslsc.ia

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.core.descargas.Progreso
import com.lhansqw.senaslsc.core.descargas.formatearDuracion
import com.lhansqw.senaslsc.core.descargas.formatearTamano
import com.lhansqw.senaslsc.core.descargas.formatearVelocidad
import com.lhansqw.senaslsc.interpreter.litertlm.DescriptorModelo
import com.lhansqw.senaslsc.interpreter.litertlm.EstadoModeloLenguaje
import com.lhansqw.senaslsc.ui.brutal.AvisoBrutal
import com.lhansqw.senaslsc.ui.brutal.BarraProgresoBrutal
import com.lhansqw.senaslsc.ui.brutal.BarraSuperiorBrutal
import com.lhansqw.senaslsc.ui.brutal.BotonBrutal
import com.lhansqw.senaslsc.ui.brutal.DialogoBrutal
import com.lhansqw.senaslsc.ui.brutal.FilaInterruptor
import com.lhansqw.senaslsc.ui.brutal.Insignia
import com.lhansqw.senaslsc.ui.brutal.TarjetaBrutal
import com.lhansqw.senaslsc.ui.brutal.TipoAviso
import com.lhansqw.senaslsc.ui.brutal.TituloSeccion
import com.lhansqw.senaslsc.ui.brutal.VarianteBoton
import com.lhansqw.senaslsc.ui.theme.ColoresBrutal
import com.lhansqw.senaslsc.ui.theme.LocalBrutal

@Composable
fun PantallaIa(onVolver: () -> Unit, viewModel: IaViewModel = viewModel()) {
    BackHandler(onBack = onVolver)
    val estado by viewModel.estadoModelo.collectAsStateWithLifecycle()
    val usar by viewModel.usarIaLocal.collectAsStateWithLifecycle()
    val consentimiento by viewModel.consentimiento.collectAsStateWithLifecycle()
    val hayClave by viewModel.hayClave.collectAsStateWithLifecycle()
    val modeloGemini by viewModel.modeloGemini.collectAsStateWithLifecycle()
    val preferencia by viewModel.preferencia.collectAsStateWithLifecycle()
    val prueba by viewModel.prueba.collectAsStateWithLifecycle()
    var confirmarDescarga by rememberSaveable { mutableStateOf(false) }
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }

    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importar)
    }

    Scaffold(
        topBar = { BarraSuperiorBrutal(stringResource(R.string.ia_titulo), onVolver) },
    ) { relleno ->
        Column(
            modifier = Modifier
                .padding(relleno)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.ia_explicacion), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.ia_privacidad),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalBrutal.current.textoSuave,
            )

            if (!viewModel.capacidad.interpreteLocal) {
                AvisoBrutal(
                    titulo = stringResource(R.string.ia_no_compatible_titulo),
                    texto = viewModel.capacidad.motivos.joinToString("\n"),
                    tipo = TipoAviso.Error,
                )
            }

            TarjetaModelo(
                descriptor = viewModel.descriptor,
                estado = estado,
                usar = usar,
                compatible = viewModel.capacidad.interpreteLocal,
                onDescargar = { confirmarDescarga = true },
                onImportar = { importar.launch(arrayOf("*/*")) },
                onCancelar = viewModel::cancelar,
                onEliminar = { confirmarEliminar = true },
                onUsar = viewModel::establecerUsarIaLocal,
            )

            TarjetaInternet(
                consentimiento = consentimiento,
                hayClave = hayClave,
                modelo = modeloGemini,
                preferencia = preferencia,
                prueba = prueba,
                onConsentimiento = viewModel::establecerConsentimiento,
                onGuardarClave = viewModel::guardarClave,
                onBorrarClave = viewModel::borrarClave,
                onModelo = viewModel::establecerModelo,
                onPreferencia = viewModel::establecerPreferencia,
                onProbar = viewModel::probar,
            )
        }
    }

    if (confirmarDescarga) {
        val uri = LocalUriHandler.current
        DialogoBrutal(
            titulo = stringResource(R.string.ia_descarga_titulo),
            texto = stringResource(
                R.string.ia_descarga_texto,
                viewModel.descriptor.nombre,
                formatearTamano(viewModel.descriptor.tamanoAproximado),
                viewModel.descriptor.licencia,
            ),
            accion = stringResource(R.string.ia_descargar),
            onAccion = {
                confirmarDescarga = false
                viewModel.descargar()
            },
            onCerrar = { confirmarDescarga = false },
            otrasAcciones = {
                BotonBrutal(
                    stringResource(R.string.ia_ver_licencia),
                    onClick = { uri.openUri(viewModel.descriptor.paginaModelo) },
                    variante = VarianteBoton.Texto,
                )
            },
        )
    }

    if (confirmarEliminar) {
        DialogoBrutal(
            titulo = stringResource(R.string.ia_eliminar_titulo),
            texto = stringResource(R.string.ia_eliminar_texto),
            accion = stringResource(R.string.ia_eliminar),
            onAccion = {
                confirmarEliminar = false
                viewModel.eliminar()
            },
            onCerrar = { confirmarEliminar = false },
            peligro = true,
        )
    }
}

@Composable
private fun TarjetaModelo(
    descriptor: DescriptorModelo,
    estado: EstadoModeloLenguaje,
    usar: Boolean,
    compatible: Boolean,
    onDescargar: () -> Unit,
    onImportar: () -> Unit,
    onCancelar: () -> Unit,
    onEliminar: () -> Unit,
    onUsar: (Boolean) -> Unit,
) {
    val brutal = LocalBrutal.current
    TarjetaBrutal(espacio = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TituloSeccion(descriptor.nombre, Modifier.weight(1f))
            if (estado is EstadoModeloLenguaje.Instalado) {
                Insignia(stringResource(R.string.ia_insignia_instalado), color = ColoresBrutal.Cian)
            } else {
                Insignia(stringResource(R.string.ia_insignia_no_instalado))
            }
        }
        Text(
            stringResource(R.string.ia_ficha, descriptor.licencia, formatearTamano(descriptor.tamanoAproximado)),
            style = MaterialTheme.typography.bodyMedium,
            color = brutal.textoSuave,
        )

        when (estado) {
            EstadoModeloLenguaje.NoInstalado -> {
                Text(stringResource(R.string.ia_estado_no_instalado))
                BotonesInstalar(compatible, stringResource(R.string.ia_descargar), onDescargar, onImportar)
            }
            is EstadoModeloLenguaje.Parcial -> {
                Text(stringResource(R.string.ia_estado_parcial, formatearTamano(estado.bytes), formatearTamano(estado.total)))
                BotonesInstalar(compatible, stringResource(R.string.ia_continuar_descarga), onDescargar, onImportar)
            }
            is EstadoModeloLenguaje.Descargando -> Avance(stringResource(R.string.ia_estado_descargando), estado.progreso, onCancelar)
            is EstadoModeloLenguaje.Importando -> Avance(stringResource(R.string.ia_estado_importando), estado.progreso, onCancelar)
            is EstadoModeloLenguaje.Instalado -> {
                Text(stringResource(R.string.ia_estado_instalado, formatearTamano(estado.bytes)))
                FilaInterruptor(
                    texto = stringResource(R.string.ia_usar),
                    activo = usar && compatible,
                    onCambio = onUsar,
                    enabled = compatible,
                )
                BotonBrutal(stringResource(R.string.ia_eliminar), onEliminar, variante = VarianteBoton.Peligro)
            }
            is EstadoModeloLenguaje.EspacioInsuficiente -> {
                AvisoBrutal(
                    stringResource(R.string.ia_estado_espacio, formatearTamano(estado.requerido), formatearTamano(estado.libre)),
                    tipo = TipoAviso.Error,
                )
                BotonesInstalar(compatible, stringResource(R.string.ia_reintentar), onDescargar, onImportar)
            }
            is EstadoModeloLenguaje.Error -> {
                AvisoBrutal(estado.mensaje, tipo = TipoAviso.Error)
                BotonesInstalar(
                    compatible,
                    stringResource(if (estado.reintentable) R.string.ia_reintentar else R.string.ia_descargar),
                    onDescargar,
                    onImportar,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BotonesInstalar(compatible: Boolean, textoDescarga: String, onDescargar: () -> Unit, onImportar: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BotonBrutal(textoDescarga, onDescargar, enabled = compatible)
        BotonBrutal(stringResource(R.string.ia_importar), onImportar, variante = VarianteBoton.Secundario, enabled = compatible)
    }
    Text(
        stringResource(R.string.ia_importar_ayuda),
        style = MaterialTheme.typography.bodySmall,
        color = LocalBrutal.current.textoSuave,
    )
}

@Composable
private fun Avance(titulo: String, progreso: Progreso, onCancelar: () -> Unit) {
    val fraccion = progreso.fraccion
    val detalle = buildList {
        add(progreso.total?.let { "${formatearTamano(progreso.bytes)} / ${formatearTamano(it)}" } ?: formatearTamano(progreso.bytes))
        if (progreso.velocidad > 0) add(formatearVelocidad(progreso.velocidad))
        progreso.segundosRestantes?.let { add(stringResource(R.string.ia_restante, formatearDuracion(it))) }
    }.joinToString(" · ")

    Text(titulo, style = MaterialTheme.typography.titleSmall)
    BarraProgresoBrutal(
        fraccion,
        Modifier
            .fillMaxWidth()
            .then(if (fraccion != null) Modifier.semantics { stateDescription = "${(fraccion * 100).toInt()} %" } else Modifier),
    )
    Text(detalle, style = MaterialTheme.typography.bodySmall)
    Text(
        stringResource(R.string.ia_no_cerrar),
        style = MaterialTheme.typography.bodySmall,
        color = LocalBrutal.current.textoSuave,
    )
    BotonBrutal(stringResource(R.string.accion_cancelar), onCancelar, variante = VarianteBoton.Secundario)
}
