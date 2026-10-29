package com.lhansqw.senaslsc.recognizer

import com.lhansqw.senaslsc.core.model.Candidata
import com.lhansqw.senaslsc.core.model.PrediccionSena
import kotlin.math.exp
import kotlin.math.ln

/**
 * Convierte la salida del modelo (logits) en una [PrediccionSena].
 *
 * - Probabilidades calibradas: softmax de `logits / temperatura`.
 * - La clase «sin seña» no aparece como candidata, pero su probabilidad sí cuenta: si el modelo
 *   cree que no hay seña, las candidatas reciben poca probabilidad.
 * - Entrada desconocida si gana la clase «sin seña» o si la **energía**
 *   (`−T · log Σ exp(logit / T)`) supera el umbral. Una energía alta indica que ninguna clase
 *   encaja bien: es la señal de que la persona hace algo que el modelo no conoce.
 */
class PostProcesador(
    private val etiquetas: List<String>,
    private val etiquetaSinSena: String?,
    private val temperatura: Float = 1f,
    private val umbralEnergia: Float? = null,
    private val maximoCandidatas: Int = 3,
) {
    init {
        require(etiquetas.isNotEmpty()) { "No hay etiquetas" }
        require(temperatura > 0f) { "La temperatura debe ser positiva" }
        require(maximoCandidatas >= 1) { "maximoCandidatas debe ser al menos 1" }
    }

    private val indiceSinSena = etiquetaSinSena?.let { etiquetas.indexOf(it) }?.takeIf { it >= 0 }

    constructor(contrato: ContratoModelo, maximoCandidatas: Int = 3) : this(
        contrato.etiquetas,
        contrato.etiquetaSinSena,
        contrato.temperatura,
        contrato.umbralEnergia,
        maximoCandidatas,
    )

    fun aplicar(logits: FloatArray, inicioMs: Long, finMs: Long): PrediccionSena {
        require(logits.size == etiquetas.size) { "El modelo devolvió ${logits.size} valores; se esperaban ${etiquetas.size}" }
        require(logits.all { it.isFinite() }) { "El modelo devolvió valores no finitos" }

        val escalados = DoubleArray(logits.size) { logits[it].toDouble() / temperatura }
        val maximo = escalados.max()
        val exponenciales = DoubleArray(escalados.size) { exp(escalados[it] - maximo) }
        val suma = exponenciales.sum()
        val probabilidades = DoubleArray(exponenciales.size) { exponenciales[it] / suma }
        val energia = (-temperatura * (maximo + ln(suma))).toFloat()

        val ganadora = probabilidades.indices.maxBy { probabilidades[it] }
        val desconocida = ganadora == indiceSinSena || (umbralEnergia != null && energia > umbralEnergia)

        val candidatas = probabilidades.indices
            .filter { it != indiceSinSena }
            .sortedByDescending { probabilidades[it] }
            .take(maximoCandidatas)
            .map { Candidata(etiquetas[it], probabilidades[it].toFloat().coerceIn(0f, 1f)) }

        return PrediccionSena(inicioMs, finMs, candidatas, desconocida)
    }

    /** Energía de unos logits, expuesta para calibrar el umbral con datos de validación. */
    fun energia(logits: FloatArray): Float {
        val escalados = DoubleArray(logits.size) { logits[it].toDouble() / temperatura }
        val maximo = escalados.max()
        return (-temperatura * (maximo + ln(escalados.sumOf { exp(it - maximo) }))).toFloat()
    }
}
