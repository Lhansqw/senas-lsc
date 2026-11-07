package com.lhansqw.senaslsc.core.texto

import platform.Foundation.NSString
import platform.Foundation.decomposedStringWithCanonicalMapping

@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun descomponerNfd(texto: String): String =
    (texto as NSString).decomposedStringWithCanonicalMapping
