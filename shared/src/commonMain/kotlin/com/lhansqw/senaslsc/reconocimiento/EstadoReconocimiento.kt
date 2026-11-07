package com.lhansqw.senaslsc.reconocimiento

import com.lhansqw.senaslsc.core.model.PrediccionSena
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.core.rendimiento.ResumenLatencia

/** Situación del modelo de señas, tal como se muestra al usuario. */
sealed interface EstadoModeloSenas {
    data object Cargando : EstadoModeloSenas

    /** Aún no hay modelo entrenado en la app (fase 6). */
    data object SinModelo : EstadoModeloSenas

    data class Listo(
        val nombre: String,
        val version: String,
        val numeroSenas: Int,
        val metricas: Map<String, Float>,
        /** Glosa de cada seña del vocabulario, para mostrar lo que el modelo está viendo. */
        val glosas: Map<String, String> = emptyMap(),
    ) : EstadoModeloSenas

    data class Invalido(val errores: List<String>) : EstadoModeloSenas
    data class Fallo(val mensaje: String) : EstadoModeloSenas
}

/** Todo lo que la pantalla necesita para mostrar el reconocimiento. Es inmutable. */
data class EstadoReconocimiento(
    val modelo: EstadoModeloSenas = EstadoModeloSenas.Cargando,
    /** Última predicción cruda del modelo (lo que «está viendo» ahora, aún sin confirmar). */
    val ultimaPrediccion: PrediccionSena? = null,
    /** Señas confirmadas de la secuencia actual. */
    val senas: List<SenaConfirmada> = emptyList(),
    val secuenciaCerrada: Boolean = false,
    /** Propuesta de frase para la última secuencia cerrada. */
    val propuesta: PropuestaFrase? = null,
    /** El modelo de lenguaje está preparando una propuesta mejor que la de reglas. */
    val generando: Boolean = false,
    /** Por qué se descartó la última respuesta del modelo de lenguaje, si se descartó. */
    val motivoDescarteIa: String? = null,
    /** Cuánto tarda el modelo de señas por ventana (últimas 120 ejecuciones), o `null` si aún no se ejecutó. */
    val latenciaModelo: ResumenLatencia? = null,
)
