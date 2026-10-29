package com.lhansqw.senaslsc.core.json

/**
 * Convierte un [Json] en texto. Lo que escribe se puede volver a leer con [LectorJson] sin pérdida
 * (salvo números no finitos, que JSON no admite y se rechazan).
 */
object EscritorJson {

    fun escribir(valor: Json): String = StringBuilder().also { escribir(valor, it) }.toString()

    private fun escribir(valor: Json, sb: StringBuilder) {
        when (valor) {
            Json.Nulo -> sb.append("null")
            is Json.Booleano -> sb.append(if (valor.valor) "true" else "false")
            is Json.Numero -> sb.append(numero(valor.valor))
            is Json.Texto -> cadena(valor.valor, sb)
            is Json.Lista -> {
                sb.append('[')
                valor.elementos.forEachIndexed { i, e ->
                    if (i > 0) sb.append(',')
                    escribir(e, sb)
                }
                sb.append(']')
            }
            is Json.Objeto -> {
                sb.append('{')
                valor.campos.entries.forEachIndexed { i, (clave, v) ->
                    if (i > 0) sb.append(',')
                    cadena(clave, sb)
                    sb.append(':')
                    escribir(v, sb)
                }
                sb.append('}')
            }
        }
    }

    private fun numero(n: Double): String {
        require(n.isFinite()) { "JSON no admite números no finitos: $n" }
        // Los enteros se escriben sin decimales (marcas de tiempo, contadores).
        return if (n == Math.floor(n) && Math.abs(n) < 1e15) n.toLong().toString() else n.toString()
    }

    private fun cadena(s: String, sb: StringBuilder) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c < ' ') sb.append(String.format("\\u%04x", c.code)) else sb.append(c)
            }
        }
        sb.append('"')
    }
}

/** Atajos para construir valores JSON en código. */
fun json(vararg campos: Pair<String, Json>): Json.Objeto = Json.Objeto(linkedMapOf(*campos))
fun String?.aJson(): Json = this?.let { Json.Texto(it) } ?: Json.Nulo
fun Number.aJson(): Json = Json.Numero(this.toDouble())
fun Boolean.aJson(): Json = Json.Booleano(this)
fun List<Json>.aJson(): Json = Json.Lista(this)
