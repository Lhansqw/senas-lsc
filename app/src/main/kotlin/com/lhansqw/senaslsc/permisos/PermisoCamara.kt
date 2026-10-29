package com.lhansqw.senaslsc.permisos

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.lhansqw.senaslsc.R

/**
 * Muestra [contenido] solo si el permiso de cámara está concedido. Si no, explica para qué se usa
 * la cámara y lo pide; si se rechazó de forma permanente, lleva a los ajustes de la app.
 *
 * Al volver de los ajustes (ON_RESUME) se comprueba otra vez, así el usuario no tiene que
 * reiniciar la app tras activarlo.
 */
@Composable
fun ConPermisoCamara(
    onVolver: () -> Unit,
    contenido: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var yaSolicitado by rememberSaveable { mutableStateOf(false) }
    // Se incrementa para forzar una nueva lectura del estado del permiso.
    var revision by remember { mutableIntStateOf(0) }

    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        yaSolicitado = true
        revision++
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { revision++ }

    val estado = remember(revision, yaSolicitado) {
        resolverEstadoPermiso(
            concedido = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
            yaSolicitado = yaSolicitado,
            debeMostrarExplicacion = context.buscarActivity()
                ?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true,
        )
    }

    when (estado) {
        EstadoPermiso.CONCEDIDO -> contenido()
        EstadoPermiso.SIN_SOLICITAR -> PantallaPermiso(
            titulo = stringResource(R.string.permiso_titulo),
            texto = stringResource(R.string.permiso_texto),
            accion = stringResource(R.string.permiso_pedir),
            onAccion = { lanzador.launch(Manifest.permission.CAMERA) },
            onVolver = onVolver,
        )
        EstadoPermiso.EXPLICAR -> PantallaPermiso(
            titulo = stringResource(R.string.permiso_titulo),
            texto = stringResource(R.string.permiso_texto_rechazado),
            accion = stringResource(R.string.permiso_pedir),
            onAccion = { lanzador.launch(Manifest.permission.CAMERA) },
            onVolver = onVolver,
        )
        EstadoPermiso.DENEGADO_PERMANENTE -> PantallaPermiso(
            titulo = stringResource(R.string.permiso_titulo_denegado),
            texto = stringResource(R.string.permiso_texto_denegado),
            accion = stringResource(R.string.permiso_abrir_ajustes),
            onAccion = { context.abrirAjustesApp() },
            onVolver = onVolver,
        )
    }
}

@Composable
private fun PantallaPermiso(
    titulo: String,
    texto: String,
    accion: String,
    onAccion: () -> Unit,
    onVolver: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = texto, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Text(
            text = stringResource(R.string.permiso_privacidad),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAccion, modifier = Modifier.fillMaxWidth()) { Text(accion) }
        OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.accion_volver))
        }
    }
}

private fun Context.buscarActivity(): Activity? {
    var actual: Context? = this
    while (actual is ContextWrapper) {
        if (actual is Activity) return actual
        actual = actual.baseContext
    }
    return null
}

private fun Context.abrirAjustesApp() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
