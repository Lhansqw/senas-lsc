package com.lhansqw.senaslsc.acerca

/** Un componente de terceros que usa la app, con su licencia. */
data class Componente(
    val nombre: String,
    /** Para qué lo usa la app, en palabras del usuario. */
    val uso: String,
    val licencia: String,
    val enlace: String,
)

/**
 * Créditos y licencias que se muestran en «Acerca de» (fase 12).
 *
 * LSC70 se publica con licencia CC BY 4.0, que **exige** citar a los autores allí donde se
 * distribuya un trabajo derivado (el modelo de señas va dentro de la app): por eso la cita está en
 * la propia app y no solo en el repositorio.
 */
object Creditos {

    const val CITA_LSC70 =
        "LSC70: conjunto de datos de Lengua de Señas Colombiana, Universidad del Cauca (2024). " +
            "Mendeley Data, doi:10.17632/9ssyn8tff5.2. Licencia CC BY 4.0. " +
            "El modelo de señas de esta app se entrenó con estos datos; no se modificaron las imágenes originales."

    val datos = Componente(
        nombre = "LSC70",
        uso = "Datos con los que se entrenó el reconocedor de señas",
        licencia = "CC BY 4.0",
        enlace = "https://data.mendeley.com/datasets/9ssyn8tff5/2",
    )

    val componentes = listOf(
        Componente(
            "MediaPipe Holistic Landmarker",
            "Detectar manos, cuerpo y rostro en la cámara",
            "Apache 2.0",
            "https://github.com/google-ai-edge/mediapipe",
        ),
        Componente(
            "LiteRT",
            "Ejecutar el modelo de señas en el teléfono",
            "Apache 2.0",
            "https://github.com/google-ai-edge/LiteRT",
        ),
        Componente(
            "LiteRT-LM",
            "Ejecutar el modelo de lenguaje en el teléfono",
            "Apache 2.0",
            "https://github.com/google-ai-edge/LiteRT-LM",
        ),
        Componente(
            "Gemma 4 E2B",
            "Proponer frases a partir de las señas (opcional, se descarga aparte)",
            "Apache 2.0",
            "https://ai.google.dev/gemma",
        ),
        Componente(
            "Android Jetpack (Compose, CameraX, Lifecycle)",
            "Interfaz y cámara",
            "Apache 2.0",
            "https://developer.android.com/jetpack",
        ),
        Componente(
            "Kotlin y kotlinx.coroutines",
            "Lenguaje y tareas en segundo plano",
            "Apache 2.0",
            "https://github.com/Kotlin/kotlinx.coroutines",
        ),
    )
}
