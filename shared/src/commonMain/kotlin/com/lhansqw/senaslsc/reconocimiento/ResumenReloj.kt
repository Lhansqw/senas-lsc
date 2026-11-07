package com.lhansqw.senaslsc.reconocimiento

/** En qué momento está el reconocimiento, tal como lo muestra el reloj. */
enum class EstadoReloj {
    /** El modelo de señas se está cargando. */
    PREPARANDO,

    /** No hay modelo o no se pudo cargar: el reloj solo muestra un aviso. */
    NO_DISPONIBLE,

    /** Se están viendo señas; la frase aún no se ha cerrado. */
    RECONOCIENDO,

    /** La frase está cerrada y hay propuesta. */
    FRASE_LISTA,
}

/**
 * Lo que necesita la pantalla del reloj (Apple Watch o Wear OS). El reloj no tiene cámara ni modelo:
 * solo muestra esto, que el teléfono le envía cada vez que cambia.
 *
 * @property frase propuesta de frase si la hay; si no, las glosas confirmadas hasta ahora («HOLA BUENAS»).
 * @property numeroSenas señas confirmadas. El reloj vibra cuando aumenta.
 */
data class ResumenReloj(
    val estado: EstadoReloj,
    val frase: String,
    val ultimaSena: String?,
    val numeroSenas: Int,
    /** La propuesta tiene dudas (seña poco segura o pregunta de aclaración). */
    val incierta: Boolean,
) {
    /**
     * Versión como diccionario de textos, para `WatchConnectivity` (iOS) y la Data Layer API (Wear OS).
     * Las claves son las de [Claves].
     */
    fun aMapa(): Map<String, String> = buildMap {
        put(Claves.ESTADO, estado.name)
        put(Claves.FRASE, frase)
        ultimaSena?.let { put(Claves.ULTIMA_SENA, it) }
        put(Claves.NUMERO_SENAS, numeroSenas.toString())
        put(Claves.INCIERTA, incierta.toString())
    }

    /** Claves del diccionario que viaja entre teléfono y reloj. */
    object Claves {
        const val ESTADO = "estado"
        const val FRASE = "frase"
        const val ULTIMA_SENA = "ultimaSena"
        const val NUMERO_SENAS = "numeroSenas"
        const val INCIERTA = "incierta"

        /** Lo añade el teléfono: `"true"` si el usuario pausó el análisis. No forma parte del resumen. */
        const val PAUSADO = "pausado"
    }

    /** Órdenes que el reloj envía al teléfono, con la clave [CLAVE]. */
    object Ordenes {
        const val CLAVE = "orden"
        const val INICIAR = "iniciar"
        const val PAUSAR = "pausar"
        const val BORRAR = "borrar"
        const val INTERPRETAR = "interpretar"
    }

    companion object {
        fun de(estado: EstadoReconocimiento): ResumenReloj {
            val senas = estado.senas
            val propuesta = estado.propuesta
            val estadoReloj = when (estado.modelo) {
                EstadoModeloSenas.Cargando -> EstadoReloj.PREPARANDO
                is EstadoModeloSenas.Listo -> if (estado.secuenciaCerrada && propuesta != null) EstadoReloj.FRASE_LISTA else EstadoReloj.RECONOCIENDO
                EstadoModeloSenas.SinModelo, is EstadoModeloSenas.Invalido, is EstadoModeloSenas.Fallo -> EstadoReloj.NO_DISPONIBLE
            }
            return ResumenReloj(
                estado = estadoReloj,
                frase = propuesta?.texto ?: senas.joinToString(" ") { it.entrada.glosa },
                ultimaSena = senas.lastOrNull()?.entrada?.glosa,
                numeroSenas = senas.size,
                incierta = propuesta?.let { it.incierta || it.preguntaAclaracion != null } ?: false,
            )
        }

        /** Lee el diccionario recibido en el reloj. Devuelve `null` si falta algo imprescindible. */
        fun deMapa(mapa: Map<String, String>): ResumenReloj? {
            val estado = mapa[Claves.ESTADO]?.let { nombre -> EstadoReloj.entries.firstOrNull { it.name == nombre } } ?: return null
            return ResumenReloj(
                estado = estado,
                frase = mapa[Claves.FRASE].orEmpty(),
                ultimaSena = mapa[Claves.ULTIMA_SENA],
                numeroSenas = mapa[Claves.NUMERO_SENAS]?.toIntOrNull() ?: 0,
                incierta = mapa[Claves.INCIERTA] == "true",
            )
        }
    }
}
