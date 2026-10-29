package com.lhansqw.senaslsc.permisos

/** Situación del permiso de cámara y lo que debe mostrar la pantalla. */
enum class EstadoPermiso {
    /** Se puede usar la cámara. */
    CONCEDIDO,

    /** Nunca se ha pedido en esta sesión: se explica el uso y se pide. */
    SIN_SOLICITAR,

    /** El usuario lo rechazó una vez: Android recomienda explicar mejor antes de volver a pedirlo. */
    EXPLICAR,

    /**
     * Rechazado con «No volver a preguntar» (o dos veces): Android ya no muestra el diálogo y
     * la única vía es activarlo en los ajustes de la app.
     */
    DENEGADO_PERMANENTE,
}

/**
 * Decide el estado a partir de lo que informa Android.
 *
 * `shouldShowRequestPermissionRationale` es `false` tanto antes de la primera petición como tras
 * un rechazo permanente; [yaSolicitado] distingue los dos casos.
 */
fun resolverEstadoPermiso(
    concedido: Boolean,
    yaSolicitado: Boolean,
    debeMostrarExplicacion: Boolean,
): EstadoPermiso = when {
    concedido -> EstadoPermiso.CONCEDIDO
    debeMostrarExplicacion -> EstadoPermiso.EXPLICAR
    yaSolicitado -> EstadoPermiso.DENEGADO_PERMANENTE
    else -> EstadoPermiso.SIN_SOLICITAR
}
