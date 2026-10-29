package com.lhansqw.senaslsc.recognizer

import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.Vocabulario

/**
 * Descripción de un modelo entrenado; se publica junto al `.tflite` como `modelo.json`.
 *
 * Fija cómo se le entregan los datos y cómo se interpreta su salida. La app comprueba con
 * [validar] que el modelo es compatible antes de usarlo: un modelo con otra versión de
 * características produciría resultados sin sentido aunque se ejecutara sin errores.
 *
 * Entrada: `[1, fotogramas, caracteristicasPorFotograma]` en float32 ([Caracteristicas]).
 * Salida: `[1, etiquetas.size]` en float32, **logits** (sin softmax), en el orden de [etiquetas].
 */
data class ContratoModelo(
    val nombre: String,
    val version: String,
    val lengua: String,
    val versionCaracteristicas: Int,
    val versionSubconjuntos: Int,
    /** Instantes por ventana tras el remuestreo. */
    val fotogramas: Int,
    /** Duración que cubre cada ventana. */
    val duracionVentanaMs: Long,
    val caracteristicasPorFotograma: Int,
    /** Identificador de cada salida del modelo, en orden. Incluye la clase «sin seña» si existe. */
    val etiquetas: List<String>,
    /** Etiqueta de la clase entrenada con movimientos que no son señas, o `null` si no hay. */
    val etiquetaSinSena: String?,
    /** Temperatura de calibración (escalado de temperatura con el conjunto de validación). */
    val temperatura: Float,
    /** Por encima de esta energía la entrada se considera desconocida; `null` para no usarla. */
    val umbralEnergia: Float?,
    /** Una entrada por cada etiqueta que no sea [etiquetaSinSena]. */
    val entradas: List<EntradaVocabulario>,
    val datosEntrenamiento: List<String>,
    /** Métricas medidas con personas no vistas en el entrenamiento (p. ej. "macro_f1" → 0,83). */
    val metricas: Map<String, Float> = emptyMap(),
) {

    /** Lista de problemas que impiden usar el modelo; vacía si es compatible. */
    fun validar(): List<String> = buildList {
        if (versionCaracteristicas != Caracteristicas.VERSION) {
            add("El modelo usa la versión $versionCaracteristicas de las características; la app usa la ${Caracteristicas.VERSION}.")
        }
        if (versionSubconjuntos != Caracteristicas.VERSION_SUBCONJUNTOS) {
            add("El modelo usa la versión $versionSubconjuntos de los subconjuntos de puntos; la app usa la ${Caracteristicas.VERSION_SUBCONJUNTOS}.")
        }
        if (caracteristicasPorFotograma != Caracteristicas.POR_FOTOGRAMA) {
            add("El modelo espera $caracteristicasPorFotograma características por fotograma; la app genera ${Caracteristicas.POR_FOTOGRAMA}.")
        }
        if (fotogramas !in 2..MAXIMO_FOTOGRAMAS) add("Número de fotogramas no válido: $fotogramas.")
        if (duracionVentanaMs !in 200..10_000) add("Duración de ventana no válida: $duracionVentanaMs ms.")
        if (!(temperatura > 0f) || temperatura.isInfinite()) add("La temperatura debe ser positiva: $temperatura.")
        if (umbralEnergia?.isFinite() == false) add("El umbral de energía no es un número válido.")

        val repetidas = etiquetas.groupBy { it }.filterValues { it.size > 1 }.keys
        if (repetidas.isNotEmpty()) add("Etiquetas repetidas: $repetidas.")
        if (etiquetaSinSena != null && etiquetaSinSena !in etiquetas) {
            add("La etiqueta «sin seña» ($etiquetaSinSena) no está entre las etiquetas.")
        }
        val senas = etiquetas.filter { it != etiquetaSinSena }
        if (senas.isEmpty()) add("El modelo no tiene ninguna seña.")
        val ids = entradas.map { it.id }.toSet()
        val sinEntrada = senas.filter { it !in ids }
        if (sinEntrada.isNotEmpty()) add("Etiquetas sin entrada de vocabulario: $sinEntrada.")
        val sobrantes = ids - senas.toSet()
        if (sobrantes.isNotEmpty()) add("Entradas de vocabulario que el modelo no produce: ${sobrantes.sorted()}.")
        val otraLengua = entradas.filter { it.lengua != lengua }.map { it.id }
        if (otraLengua.isNotEmpty()) add("Entradas de otra lengua: $otraLengua.")
    }

    /** Vocabulario publicado en la app. Solo se debe llamar si [validar] no devolvió problemas. */
    fun vocabulario(): Vocabulario = Vocabulario(lengua, entradas)

    fun ficha(): FichaModelo = FichaModelo(nombre, version, lengua, datosEntrenamiento)

    companion object {
        const val MAXIMO_FOTOGRAMAS = 256
    }
}
