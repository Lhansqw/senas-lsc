package com.lhansqw.senaslsc.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Paleta propia: azul profundo y turquesa, con contraste AA sobre fondo claro y oscuro.
private val Azul = Color(0xFF1F4E8C)
private val AzulClaro = Color(0xFFAAC7FF)
private val Turquesa = Color(0xFF00696B)
private val TurquesaClaro = Color(0xFF4CDADC)

private val EsquemaClaro = lightColorScheme(
    primary = Azul,
    secondary = Turquesa,
)

private val EsquemaOscuro = darkColorScheme(
    primary = AzulClaro,
    secondary = TurquesaClaro,
)

@Composable
fun TemaSenasLSC(
    oscuro: Boolean = isSystemInDarkTheme(),
    // Color dinámico (Material You) en Android 12+; desactivable para mantener la identidad visual.
    colorDinamico: Boolean = true,
    contenido: @Composable () -> Unit,
) {
    val esquema = when {
        colorDinamico && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (oscuro) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        oscuro -> EsquemaOscuro
        else -> EsquemaClaro
    }

    MaterialTheme(colorScheme = esquema, content = contenido)
}
