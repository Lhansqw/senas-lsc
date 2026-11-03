package com.lhansqw.senaslsc.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lhansqw.senaslsc.R

/**
 * Atkinson Hyperlegible Next (SIL OFL 1.1): diseñada por el Braille Institute para que cada letra
 * se distinga aunque se lea con baja visión o de reojo, como pasa al hacer señas frente al teléfono.
 * El archivo es variable: cada peso es la misma fuente con otro valor del eje «wght».
 */
private fun peso(w: FontWeight) = Font(
    R.font.atkinson_hyperlegible_next,
    weight = w,
    variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)),
)

val Atkinson = FontFamily(
    peso(FontWeight.Normal),
    peso(FontWeight.Medium),
    peso(FontWeight.SemiBold),
    peso(FontWeight.Bold),
    peso(FontWeight.ExtraBold),
)

private val base = Typography()

private fun TextStyle.conAtkinson() = copy(fontFamily = Atkinson)

/** La escala de Material 3 con Atkinson en todos los estilos. */
val TipografiaSenas = Typography(
    displayLarge = base.displayLarge.conAtkinson(),
    displayMedium = base.displayMedium.conAtkinson(),
    displaySmall = base.displaySmall.conAtkinson(),
    headlineLarge = base.headlineLarge.conAtkinson(),
    headlineMedium = base.headlineMedium.conAtkinson(),
    headlineSmall = base.headlineSmall.conAtkinson(),
    titleLarge = base.titleLarge.conAtkinson(),
    titleMedium = base.titleMedium.conAtkinson(),
    titleSmall = base.titleSmall.conAtkinson(),
    bodyLarge = base.bodyLarge.conAtkinson(),
    bodyMedium = base.bodyMedium.conAtkinson(),
    bodySmall = base.bodySmall.conAtkinson(),
    labelLarge = base.labelLarge.conAtkinson(),
    labelMedium = base.labelMedium.conAtkinson(),
    labelSmall = base.labelSmall.conAtkinson(),
)

/** Estilos propios de la vista de cámara, fuera de la escala de Material. */
object EstiloCamara {
    /** La frase propuesta: lo que lee la otra persona. Grande, en papel. */
    val frase = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp)

    /** La seña que el modelo está viendo ahora, aún sin confirmar. */
    val glosaEnCurso = TextStyle(
        fontFamily = Atkinson, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 34.sp,
        letterSpacing = 0.02.em,
    )

    /** Glosa de una seña ya confirmada en la secuencia. */
    val glosa = TextStyle(
        fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp,
        letterSpacing = 0.03.em,
    )

    /** Cifras que cambian a cada fotograma: anchos fijos para que el texto no baile. */
    val cifras = TextStyle(
        fontFamily = Atkinson, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp,
        fontFeatureSettings = "tnum",
    )

    val nota = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
}
