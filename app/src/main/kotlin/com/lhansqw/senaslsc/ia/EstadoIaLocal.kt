package com.lhansqw.senaslsc.ia

/** Si el modelo de lenguaje del teléfono se puede usar para mejorar las frases, y por qué no. */
sealed interface EstadoIaLocal {
    /** El teléfono no tiene memoria suficiente (u otro requisito). */
    data class NoCompatible(val motivo: String) : EstadoIaLocal

    /** El modelo no está instalado. */
    data object NoInstalada : EstadoIaLocal

    /** Instalado, pero el usuario la desactivó. */
    data object Desactivada : EstadoIaLocal

    data object Cargando : EstadoIaLocal
    data object Lista : EstadoIaLocal
    data class Error(val mensaje: String) : EstadoIaLocal

    /** Solo en este estado se piden frases al modelo. */
    val disponible: Boolean get() = this == Lista
}

/**
 * Estado de partida antes de cargar el modelo. Si devuelve [EstadoIaLocal.Cargando], quien llama
 * debe cargar el modelo y pasar a [EstadoIaLocal.Lista] o [EstadoIaLocal.Error].
 *
 * Orden de prioridad: compatibilidad del teléfono → instalación → preferencia del usuario.
 */
fun decidirEstadoIa(
    compatible: Boolean,
    motivoNoCompatible: String?,
    instalada: Boolean,
    activadaPorUsuario: Boolean,
): EstadoIaLocal = when {
    !compatible -> EstadoIaLocal.NoCompatible(motivoNoCompatible ?: "Este teléfono no cumple los requisitos del modelo de lenguaje.")
    !instalada -> EstadoIaLocal.NoInstalada
    !activadaPorUsuario -> EstadoIaLocal.Desactivada
    else -> EstadoIaLocal.Cargando
}
