package com.lhansqw.senaslsc.core.descargas

/** Cabecera `Content-Range: bytes inicio-fin/total` (el total puede ser `*`). */
data class RangoContenido(val inicio: Long, val fin: Long, val total: Long?)

object Reanudacion {

    private val PATRON = Regex("""^\s*bytes\s+(\d+)-(\d+)/(\d+|\*)\s*$""")

    /** Lee `Content-Range`, o devuelve `null` si no tiene el formato esperado. */
    fun leerRango(cabecera: String?): RangoContenido? {
        val m = PATRON.matchEntire(cabecera ?: return null) ?: return null
        val inicio = m.groupValues[1].toLongOrNull() ?: return null
        val fin = m.groupValues[2].toLongOrNull() ?: return null
        val total = m.groupValues[3].takeIf { it != "*" }?.toLongOrNull()
        if (fin < inicio || (total != null && fin >= total)) return null
        return RangoContenido(inicio, fin, total)
    }

    /** Cabecera `Range` para continuar desde [bytesDescargados], o `null` si se empieza de cero. */
    fun cabeceraRango(bytesDescargados: Long): String? =
        if (bytesDescargados > 0) "bytes=$bytesDescargados-" else null

    /**
     * Decide qué hacer con la respuesta del servidor a una petición (con o sin `Range`).
     *
     * @param bytesDescargados tamaño del archivo parcial que ya hay en el teléfono.
     * @param codigo código HTTP de la respuesta.
     * @param contentRange cabecera `Content-Range` de la respuesta.
     * @param contentLength cabecera `Content-Length`, o −1 si no viene.
     */
    fun decidir(bytesDescargados: Long, codigo: Int, contentRange: String?, contentLength: Long): Decision {
        require(bytesDescargados >= 0) { "bytesDescargados no puede ser negativo" }
        return when (codigo) {
            200 -> Decision.EmpezarDeCero(total = contentLength.takeIf { it > 0 })
            206 -> {
                val rango = leerRango(contentRange)
                    ?: return Decision.Fallo("El servidor envió un rango que no se entiende: $contentRange")
                if (rango.inicio != bytesDescargados) {
                    // El servidor no continuó donde se pidió: lo seguro es empezar de cero.
                    Decision.Fallo("El servidor continuó en el byte ${rango.inicio} en lugar del $bytesDescargados")
                } else {
                    Decision.Continuar(desde = bytesDescargados, total = rango.total)
                }
            }
            // 416: el rango pedido no existe; normalmente, porque el archivo ya está completo.
            416 -> {
                val total = leerTotal(contentRange)
                if (total != null && total == bytesDescargados) Decision.YaCompleto(total) else Decision.EmpezarDeCero(null, descartarParcial = true)
            }
            in 400..499 -> Decision.Fallo(
                when (codigo) {
                    401, 403 -> "El servidor no permite descargar el modelo (código $codigo). Puede requerir aceptar su licencia: impórtalo desde un archivo."
                    404 -> "El modelo ya no está en esa dirección (código 404)."
                    else -> "El servidor rechazó la descarga (código $codigo)."
                },
            )
            in 500..599 -> Decision.Fallo("El servidor tiene un problema (código $codigo). Inténtalo más tarde.", reintentable = true)
            else -> Decision.Fallo("Respuesta inesperada del servidor (código $codigo).")
        }
    }

    /** Total de `Content-Range: bytes * /1234` (forma usada con el código 416). */
    private fun leerTotal(cabecera: String?): Long? =
        cabecera?.let { Regex("""/(\d+)\s*$""").find(it)?.groupValues?.get(1)?.toLongOrNull() }

    sealed interface Decision {
        /** Se descarga desde el principio; si había un parcial, se descarta. */
        data class EmpezarDeCero(val total: Long?, val descartarParcial: Boolean = true) : Decision
        data class Continuar(val desde: Long, val total: Long?) : Decision
        data class YaCompleto(val total: Long) : Decision
        data class Fallo(val mensaje: String, val reintentable: Boolean = false) : Decision
    }
}
