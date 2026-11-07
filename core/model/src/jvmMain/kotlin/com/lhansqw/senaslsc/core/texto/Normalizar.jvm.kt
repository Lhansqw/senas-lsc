package com.lhansqw.senaslsc.core.texto

import java.text.Normalizer

internal actual fun descomponerNfd(texto: String): String = Normalizer.normalize(texto, Normalizer.Form.NFD)
