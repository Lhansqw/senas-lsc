package com.lhansqw.senaslsc.dispositivo

/**
 * Datos del teléfono que deciden qué componentes de IA se pueden ejecutar en local.
 * No depende de Android, así que se prueba con JUnit en el ordenador.
 */
data class PerfilDispositivo(
    val nivelApi: Int,
    val ramTotalMb: Long,
    val esDispositivoBajaMemoria: Boolean,
    val nucleos: Int,
)

data class CapacidadDispositivo(
    /** MediaPipe + reconocedor LiteRT: modelos pequeños, deben funcionar en todo teléfono compatible. */
    val reconocedorLocal: Boolean,
    /** Modelo de lenguaje local (Gemma con LiteRT-LM): solo en teléfonos con memoria suficiente. */
    val interpreteLocal: Boolean,
    /** Explicaciones legibles para mostrar al usuario cuando algo no está disponible. */
    val motivos: List<String>,
)

object EvaluadorCapacidad {

    /** Android 8.0: mínimo de la app y de LiteRT-LM. */
    const val API_MINIMA = 26

    /**
     * RAM total mínima para ofrecer el modelo de lenguaje local.
     *
     * Valor provisional: se ajusta en la fase 8 midiendo el consumo real del modelo en un teléfono.
     * Android informa algo menos que la RAM nominal (un teléfono de 6 GB suele reportar unos
     * 5,5–5,8 GB), por eso el umbral es de 5 500 MB y no de 6 144 MB.
     */
    const val RAM_MINIMA_INTERPRETE_MB = 5_500L

    /** Núcleos mínimos para ejecutar cámara, MediaPipe y reconocedor sin bloquear la interfaz. */
    const val NUCLEOS_MINIMOS = 4

    fun evaluar(perfil: PerfilDispositivo): CapacidadDispositivo {
        val motivos = mutableListOf<String>()

        val apiValida = perfil.nivelApi >= API_MINIMA
        if (!apiValida) {
            motivos += "Se necesita Android 8.0 (API $API_MINIMA) o superior."
        }

        val nucleosValidos = perfil.nucleos >= NUCLEOS_MINIMOS
        if (!nucleosValidos) {
            motivos += "El reconocimiento en tiempo real necesita al menos $NUCLEOS_MINIMOS núcleos."
        }
        val reconocedor = apiValida && nucleosValidos

        val interprete = when {
            !reconocedor -> false
            perfil.esDispositivoBajaMemoria -> {
                motivos += "El sistema marca este teléfono como de baja memoria: el modelo de " +
                    "lenguaje local no está disponible y se usará el intérprete por reglas."
                false
            }
            perfil.ramTotalMb < RAM_MINIMA_INTERPRETE_MB -> {
                motivos += "El modelo de lenguaje local necesita unos ${RAM_MINIMA_INTERPRETE_MB / 1024 + 1} GB " +
                    "de RAM (este teléfono tiene ${formatearGb(perfil.ramTotalMb)}). " +
                    "Se usará el intérprete por reglas."
                false
            }
            else -> true
        }

        return CapacidadDispositivo(
            reconocedorLocal = reconocedor,
            interpreteLocal = interprete,
            motivos = motivos,
        )
    }

    /** 5 734 MB → "5,6 GB" (coma decimal, como se escribe en español). */
    fun formatearGb(mb: Long): String {
        val decimas = Math.round(mb / 1024.0 * 10).toInt()
        return "${decimas / 10},${decimas % 10} GB"
    }
}
