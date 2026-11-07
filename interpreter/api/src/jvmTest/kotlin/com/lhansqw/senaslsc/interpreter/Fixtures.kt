package com.lhansqw.senaslsc.interpreter

import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.core.model.TipoSena

internal fun sena(
    id: String,
    confianza: Float = 0.9f,
    tipo: TipoSena = TipoSena.PALABRA,
    interrogativa: Boolean = false,
    significado: String = id.substringAfter('.'),
): SenaConfirmada {
    val entrada = EntradaVocabulario(id, id.substringAfter('.').uppercase(), significado, tipo, interrogativa)
    return SenaConfirmada(entrada, confianza, inicioMs = 0, finMs = 100)
}
