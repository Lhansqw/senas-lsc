package com.lhansqw.senaslsc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Paleta neo-brutalista de las pantallas de menú: colores planos sobre papel, bordes de tinta y
 * sombras duras. Sobre cian, magenta y amarillo el texto va siempre en tinta (contraste AA o más).
 */
object ColoresBrutal {
    val Papel = Color(0xFFF4EFE3)
    val Blanco = Color(0xFFFFFFFF)
    val Tinta = Color(0xFF111111)
    val TintaSuave = Color(0xFF4A4A4A)

    val Cian = Color(0xFF00D1E0)
    val Magenta = Color(0xFFFF3EA5)
    val Amarillo = Color(0xFFFFD400)
    /** Rojo de error: lleva texto blanco (5,2:1). */
    val Rojo = Color(0xFFD7191F)

    val Noche = Color(0xFF141414)
    val NocheSuperficie = Color(0xFF1F1F1F)
    val PapelSuave = Color(0xFFB9B4A9)
}

/** Medidas y colores del trazo brutalista, que cambian con el modo claro u oscuro. */
@Immutable
data class Brutal(
    val fondo: Color,
    val superficie: Color,
    val texto: Color,
    val textoSuave: Color,
    /** Bordes, separadores y sombras. */
    val trazo: Color,
    /** Color del texto de los enlaces y del contorno de foco. */
    val enlace: Color,
    val foco: Color,
    val grosor: Dp = 3.dp,
    val sombra: Dp = 4.dp,
)

private val BrutalClaro = Brutal(
    fondo = ColoresBrutal.Papel,
    superficie = ColoresBrutal.Blanco,
    texto = ColoresBrutal.Tinta,
    textoSuave = ColoresBrutal.TintaSuave,
    trazo = ColoresBrutal.Tinta,
    enlace = ColoresBrutal.Tinta,
    foco = ColoresBrutal.Magenta,
)

// En oscuro el papel pasa a ser el trazo: una sombra negra no se vería sobre el fondo casi negro.
private val BrutalOscuro = Brutal(
    fondo = ColoresBrutal.Noche,
    superficie = ColoresBrutal.NocheSuperficie,
    texto = ColoresBrutal.Papel,
    textoSuave = ColoresBrutal.PapelSuave,
    trazo = ColoresBrutal.Papel,
    enlace = ColoresBrutal.Cian,
    foco = ColoresBrutal.Amarillo,
)

val LocalBrutal = staticCompositionLocalOf { BrutalClaro }

private val EsquemaClaro = lightColorScheme(
    primary = ColoresBrutal.Cian,
    onPrimary = ColoresBrutal.Tinta,
    primaryContainer = ColoresBrutal.Cian,
    onPrimaryContainer = ColoresBrutal.Tinta,
    secondary = ColoresBrutal.Magenta,
    onSecondary = ColoresBrutal.Tinta,
    secondaryContainer = ColoresBrutal.Magenta,
    onSecondaryContainer = ColoresBrutal.Tinta,
    tertiary = ColoresBrutal.Amarillo,
    onTertiary = ColoresBrutal.Tinta,
    tertiaryContainer = ColoresBrutal.Amarillo,
    onTertiaryContainer = ColoresBrutal.Tinta,
    error = ColoresBrutal.Rojo,
    onError = ColoresBrutal.Blanco,
    errorContainer = ColoresBrutal.Rojo,
    onErrorContainer = ColoresBrutal.Blanco,
    background = ColoresBrutal.Papel,
    onBackground = ColoresBrutal.Tinta,
    surface = ColoresBrutal.Papel,
    onSurface = ColoresBrutal.Tinta,
    surfaceVariant = ColoresBrutal.Blanco,
    onSurfaceVariant = ColoresBrutal.TintaSuave,
    surfaceContainerHighest = ColoresBrutal.Blanco,
    surfaceContainerHigh = ColoresBrutal.Blanco,
    surfaceContainer = ColoresBrutal.Blanco,
    surfaceContainerLow = ColoresBrutal.Blanco,
    surfaceContainerLowest = ColoresBrutal.Blanco,
    outline = ColoresBrutal.Tinta,
    outlineVariant = ColoresBrutal.Tinta,
)

private val EsquemaOscuro = darkColorScheme(
    primary = ColoresBrutal.Cian,
    onPrimary = ColoresBrutal.Tinta,
    primaryContainer = ColoresBrutal.Cian,
    onPrimaryContainer = ColoresBrutal.Tinta,
    secondary = ColoresBrutal.Magenta,
    onSecondary = ColoresBrutal.Tinta,
    secondaryContainer = ColoresBrutal.Magenta,
    onSecondaryContainer = ColoresBrutal.Tinta,
    tertiary = ColoresBrutal.Amarillo,
    onTertiary = ColoresBrutal.Tinta,
    tertiaryContainer = ColoresBrutal.Amarillo,
    onTertiaryContainer = ColoresBrutal.Tinta,
    error = ColoresBrutal.Rojo,
    onError = ColoresBrutal.Blanco,
    errorContainer = ColoresBrutal.Rojo,
    onErrorContainer = ColoresBrutal.Blanco,
    background = ColoresBrutal.Noche,
    onBackground = ColoresBrutal.Papel,
    surface = ColoresBrutal.Noche,
    onSurface = ColoresBrutal.Papel,
    surfaceVariant = ColoresBrutal.NocheSuperficie,
    onSurfaceVariant = ColoresBrutal.PapelSuave,
    surfaceContainerHighest = ColoresBrutal.NocheSuperficie,
    surfaceContainerHigh = ColoresBrutal.NocheSuperficie,
    surfaceContainer = ColoresBrutal.NocheSuperficie,
    surfaceContainerLow = ColoresBrutal.NocheSuperficie,
    surfaceContainerLowest = ColoresBrutal.NocheSuperficie,
    outline = ColoresBrutal.Papel,
    outlineVariant = ColoresBrutal.Papel,
)

/** Esquinas rectas: lo único redondeado que queda es un radio mínimo para que no corte en pantallas curvas. */
private val FormasRectas = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp),
)

/** Tema de las pantallas de menú. Sin color dinámico: Material You reemplazaría la paleta. */
@Composable
fun TemaSenasLSC(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBrutal provides if (oscuro) BrutalOscuro else BrutalClaro) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            typography = TipografiaSenas,
            shapes = FormasRectas,
            content = contenido,
        )
    }
}

// La vista de cámara conserva su identidad azul y turquesa: velo translúcido sobre la imagen.
private val EsquemaCamara = darkColorScheme(
    primary = Color(0xFFAAC7FF),
    secondary = Color(0xFF4CDADC),
)

/** Tema fijo de la vista de cámara, independiente del rediseño de los menús. */
@Composable
fun TemaCamara(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EsquemaCamara, typography = TipografiaCamara, content = contenido)
}
