package com.lhansqw.senaslsc.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colores de la vista de cámara. Hay dos materiales: el velo de tinta azul, translúcido, para lo que
 * vive sobre la imagen (señas, controles), y el papel claro para la frase propuesta, que es un texto
 * aparte que la persona revisa antes de usarlo.
 */
object ColoresCamara {
    /** Tinta azul de la marca; con transparencia deja ver la cámara detrás. */
    val Tinta = Color(0xFF0F2233)
    val Velo = Tinta.copy(alpha = 0.78f)

    val Texto = Color.White
    val TextoSecundario = Color(0xFFC3D0D8)
    val TextoApagado = Color(0xFF8A9BA6)

    /** Señas: la que se está viendo y las confirmadas. */
    val Turquesa = Color(0xFF4CDADC)
    val PistaConfianza = Color(0x33FFFFFF)

    /** Incertidumbre sobre el velo. */
    val Ambar = Color(0xFFFFD54F)

    /** La tira de papel de la frase propuesta. */
    val Papel = Color(0xFFF3F6F5)
    val PapelTexto = Tinta
    val PapelTextoSecundario = Color(0xFF4A5C67)
    val PapelAccion = Color(0xFF1F4E8C)

    /** Incertidumbre sobre papel: ámbar oscuro, con contraste AA. */
    val PapelAviso = Color(0xFF7A4F00)
    val PapelAvisoFondo = Color(0xFFFFF1C7)
}
