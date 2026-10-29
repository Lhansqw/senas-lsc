package com.lhansqw.senaslsc.core.json

/**
 * Lectura de campos con mensajes de error claros. Cada función lanza [CampoInvalido] indicando
 * la ruta del campo, para que el usuario sepa qué corregir en el archivo.
 */
class CampoInvalido(val ruta: String, mensaje: String) : IllegalArgumentException("$ruta: $mensaje")

fun Json.Objeto.texto(clave: String, ruta: String = clave): String =
    when (val v = this[clave]) {
        is Json.Texto -> v.valor
        null -> throw CampoInvalido(ruta, "falta el campo")
        else -> throw CampoInvalido(ruta, "debe ser un texto")
    }

fun Json.Objeto.textoOpcional(clave: String, ruta: String = clave): String? =
    when (val v = this[clave]) {
        is Json.Texto -> v.valor
        null, Json.Nulo -> null
        else -> throw CampoInvalido(ruta, "debe ser un texto o null")
    }

fun Json.Objeto.numero(clave: String, ruta: String = clave): Double =
    when (val v = this[clave]) {
        is Json.Numero -> v.valor
        null -> throw CampoInvalido(ruta, "falta el campo")
        else -> throw CampoInvalido(ruta, "debe ser un número")
    }

fun Json.Objeto.numeroOpcional(clave: String, ruta: String = clave): Double? =
    when (val v = this[clave]) {
        is Json.Numero -> v.valor
        null, Json.Nulo -> null
        else -> throw CampoInvalido(ruta, "debe ser un número o null")
    }

fun Json.Objeto.entero(clave: String, ruta: String = clave): Long {
    val n = numero(clave, ruta)
    if (n != Math.floor(n) || n.isInfinite() || n > Long.MAX_VALUE || n < Long.MIN_VALUE) {
        throw CampoInvalido(ruta, "debe ser un número entero")
    }
    return n.toLong()
}

fun Json.Objeto.booleano(clave: String, ruta: String = clave): Boolean =
    when (val v = this[clave]) {
        is Json.Booleano -> v.valor
        null -> throw CampoInvalido(ruta, "falta el campo")
        else -> throw CampoInvalido(ruta, "debe ser true o false")
    }

fun Json.Objeto.booleanoOpcional(clave: String, porDefecto: Boolean, ruta: String = clave): Boolean =
    when (val v = this[clave]) {
        is Json.Booleano -> v.valor
        null, Json.Nulo -> porDefecto
        else -> throw CampoInvalido(ruta, "debe ser true o false")
    }

fun Json.Objeto.lista(clave: String, ruta: String = clave): List<Json> =
    when (val v = this[clave]) {
        is Json.Lista -> v.elementos
        null -> throw CampoInvalido(ruta, "falta el campo")
        else -> throw CampoInvalido(ruta, "debe ser una lista")
    }

fun Json.Objeto.listaTextos(clave: String, ruta: String = clave): List<String> =
    lista(clave, ruta).mapIndexed { i, e -> (e as? Json.Texto)?.valor ?: throw CampoInvalido("$ruta[$i]", "debe ser un texto") }

fun Json.Objeto.objeto(clave: String, ruta: String = clave): Json.Objeto =
    when (val v = this[clave]) {
        is Json.Objeto -> v
        null -> throw CampoInvalido(ruta, "falta el campo")
        else -> throw CampoInvalido(ruta, "debe ser un objeto")
    }

fun Json.Objeto.objetoOpcional(clave: String, ruta: String = clave): Json.Objeto? =
    when (val v = this[clave]) {
        is Json.Objeto -> v
        null, Json.Nulo -> null
        else -> throw CampoInvalido(ruta, "debe ser un objeto o null")
    }

/** Convierte un elemento de lista en objeto, o falla con su ruta. */
fun Json.comoObjeto(ruta: String): Json.Objeto = this as? Json.Objeto ?: throw CampoInvalido(ruta, "debe ser un objeto")
