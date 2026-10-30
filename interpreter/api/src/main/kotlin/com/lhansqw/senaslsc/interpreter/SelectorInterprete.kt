package com.lhansqw.senaslsc.interpreter

/** Qué prefiere el usuario para mejorar las frases (fase 10). */
enum class PreferenciaInterprete {
    /** IA del teléfono si está lista; si no, la de internet (si está permitida); si no, reglas. */
    AUTOMATICO,

    /** Nunca enviar nada a internet. */
    SOLO_TELEFONO,

    /** IA de internet si hay conexión; si no, la del teléfono; si no, reglas. */
    PREFERIR_INTERNET,
}

/** Intérprete que se usará para la siguiente frase. */
enum class EleccionInterprete { TELEFONO, INTERNET, REGLAS }

/** Lo que se sabe en el momento de pedir una frase. */
data class SituacionInterprete(
    val telefonoListo: Boolean,
    /** El usuario dio su consentimiento y hay una clave guardada. */
    val internetPermitido: Boolean,
    val hayConexion: Boolean,
)

/**
 * Decide qué intérprete usar (fase 10). Reglas fijas y sin sorpresas:
 * - Sin consentimiento nunca se usa internet, sea cual sea la preferencia.
 * - El intérprete por reglas siempre está disponible: es el último recurso y también la red de
 *   seguridad cuando un modelo de lenguaje falla o no cumple el contrato.
 */
object SelectorInterprete {

    fun elegir(preferencia: PreferenciaInterprete, s: SituacionInterprete): EleccionInterprete {
        val internet = s.internetPermitido && s.hayConexion
        return when (preferencia) {
            PreferenciaInterprete.SOLO_TELEFONO -> if (s.telefonoListo) EleccionInterprete.TELEFONO else EleccionInterprete.REGLAS
            PreferenciaInterprete.AUTOMATICO -> when {
                s.telefonoListo -> EleccionInterprete.TELEFONO
                internet -> EleccionInterprete.INTERNET
                else -> EleccionInterprete.REGLAS
            }
            PreferenciaInterprete.PREFERIR_INTERNET -> when {
                internet -> EleccionInterprete.INTERNET
                s.telefonoListo -> EleccionInterprete.TELEFONO
                else -> EleccionInterprete.REGLAS
            }
        }
    }
}
