package com.lhansqw.senaslsc.core.texto

import java.text.Normalizer

/**
 * Normalización para búsquedas: minúsculas, sin tildes ni diéresis y con espacios simples.
 * «Cómo», «COMO» y «como» se consideran iguales; la «ñ» se conserva, porque en español es otra
 * letra y no una «n» con tilde.
 */
fun normalizarParaBusqueda(texto: String): String {
    val conMarcaEnie = texto.replace('ñ', '\u0001').replace('Ñ', '\u0002')
    val sinMarcas = Normalizer.normalize(conMarcaEnie, Normalizer.Form.NFD).replace(MARCAS, "")
    return sinMarcas
        .replace('\u0001', 'ñ')
        .replace('\u0002', 'ñ')
        .lowercase()
        .trim()
        .replace(ESPACIOS, " ")
}

/** `true` si todas las palabras de [consulta] aparecen en [texto], sin importar tildes ni mayúsculas. */
fun coincideBusqueda(texto: String, consulta: String): Boolean {
    val palabras = normalizarParaBusqueda(consulta).split(' ').filter { it.isNotEmpty() }
    if (palabras.isEmpty()) return true
    val normalizado = normalizarParaBusqueda(texto)
    return palabras.all { it in normalizado }
}

private val MARCAS = Regex("\\p{Mn}+")
private val ESPACIOS = Regex("\\s+")
