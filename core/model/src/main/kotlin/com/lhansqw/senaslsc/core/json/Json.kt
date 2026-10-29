package com.lhansqw.senaslsc.core.json

/**
 * Valor JSON. Lector mínimo y estricto (RFC 8259) sin dependencias, para poder probarlo en la JVM
 * y usarlo en Android sin `org.json`.
 */
sealed interface Json {
    data class Objeto(val campos: Map<String, Json>) : Json {
        operator fun get(clave: String): Json? = campos[clave]
    }

    data class Lista(val elementos: List<Json>) : Json
    data class Texto(val valor: String) : Json
    data class Numero(val valor: Double) : Json
    data class Booleano(val valor: Boolean) : Json
    data object Nulo : Json
}

/** Error de sintaxis con la posición (carácter) donde se detectó. */
class JsonInvalido(mensaje: String, val posicion: Int) : IllegalArgumentException("$mensaje (posición $posicion)")

object LectorJson {

    /** Lee un documento JSON completo. Lanza [JsonInvalido] si no es válido. */
    fun leer(texto: String): Json {
        val lector = Cursor(texto)
        lector.saltarEspacios()
        val valor = lector.valor(profundidad = 0)
        lector.saltarEspacios()
        if (!lector.fin) throw JsonInvalido("Hay contenido después del valor JSON", lector.pos)
        return valor
    }

    /**
     * Busca el primer objeto JSON completo dentro de un texto más largo. Los modelos de lenguaje
     * a veces añaden texto o marcas de código alrededor de su respuesta aunque se les pida solo
     * JSON. Devuelve `null` si no hay ningún objeto válido.
     */
    fun extraerPrimerObjeto(texto: String): Json.Objeto? {
        var inicio = texto.indexOf('{')
        while (inicio >= 0) {
            val fin = finDeObjeto(texto, inicio)
            if (fin != null) {
                val candidato = runCatching { leer(texto.substring(inicio, fin + 1)) }.getOrNull()
                if (candidato is Json.Objeto) return candidato
            }
            inicio = texto.indexOf('{', inicio + 1)
        }
        return null
    }

    /** Posición de la llave que cierra el objeto que empieza en [inicio], respetando cadenas. */
    private fun finDeObjeto(texto: String, inicio: Int): Int? {
        var nivel = 0
        var enCadena = false
        var escapado = false
        for (i in inicio until texto.length) {
            val c = texto[i]
            when {
                escapado -> escapado = false
                enCadena && c == '\\' -> escapado = true
                c == '"' -> enCadena = !enCadena
                enCadena -> Unit
                c == '{' -> nivel++
                c == '}' -> {
                    nivel--
                    if (nivel == 0) return i
                }
            }
        }
        return null
    }

    private const val PROFUNDIDAD_MAXIMA = 64

    private class Cursor(private val s: String) {
        var pos = 0
        val fin: Boolean get() = pos >= s.length

        fun saltarEspacios() {
            while (pos < s.length && s[pos] in " \t\n\r") pos++
        }

        fun valor(profundidad: Int): Json {
            if (profundidad > PROFUNDIDAD_MAXIMA) throw JsonInvalido("Anidamiento demasiado profundo", pos)
            if (fin) throw JsonInvalido("Se esperaba un valor y terminó el texto", pos)
            return when (val c = s[pos]) {
                '{' -> objeto(profundidad)
                '[' -> lista(profundidad)
                '"' -> Json.Texto(cadena())
                't' -> literal("true", Json.Booleano(true))
                'f' -> literal("false", Json.Booleano(false))
                'n' -> literal("null", Json.Nulo)
                else -> if (c == '-' || c.isDigit()) numero() else throw JsonInvalido("Carácter inesperado '$c'", pos)
            }
        }

        private fun objeto(profundidad: Int): Json.Objeto {
            pos++ // {
            val campos = LinkedHashMap<String, Json>()
            saltarEspacios()
            if (consumir('}')) return Json.Objeto(campos)
            while (true) {
                saltarEspacios()
                if (fin || s[pos] != '"') throw JsonInvalido("Se esperaba el nombre de un campo", pos)
                val clave = cadena()
                saltarEspacios()
                if (!consumir(':')) throw JsonInvalido("Se esperaba ':'", pos)
                saltarEspacios()
                campos[clave] = valor(profundidad + 1)
                saltarEspacios()
                if (consumir('}')) return Json.Objeto(campos)
                if (!consumir(',')) throw JsonInvalido("Se esperaba ',' o '}'", pos)
            }
        }

        private fun lista(profundidad: Int): Json.Lista {
            pos++ // [
            val elementos = mutableListOf<Json>()
            saltarEspacios()
            if (consumir(']')) return Json.Lista(elementos)
            while (true) {
                saltarEspacios()
                elementos += valor(profundidad + 1)
                saltarEspacios()
                if (consumir(']')) return Json.Lista(elementos)
                if (!consumir(',')) throw JsonInvalido("Se esperaba ',' o ']'", pos)
            }
        }

        private fun cadena(): String {
            pos++ // "
            val sb = StringBuilder()
            while (true) {
                if (fin) throw JsonInvalido("Cadena sin cerrar", pos)
                val c = s[pos++]
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (fin) throw JsonInvalido("Escape incompleto", pos)
                        when (val e = s[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > s.length) throw JsonInvalido("Escape \\u incompleto", pos)
                                val hex = s.substring(pos, pos + 4)
                                val codigo = hex.toIntOrNull(16) ?: throw JsonInvalido("Escape \\u no válido: $hex", pos)
                                sb.append(codigo.toChar())
                                pos += 4
                            }
                            else -> throw JsonInvalido("Escape no válido: \\$e", pos - 1)
                        }
                    }
                    c < ' ' -> throw JsonInvalido("Carácter de control sin escapar en una cadena", pos - 1)
                    else -> sb.append(c)
                }
            }
        }

        private fun numero(): Json.Numero {
            val inicio = pos
            consumir('-')
            if (fin) throw JsonInvalido("Número incompleto", pos)
            if (s[pos] == '0') {
                pos++
            } else if (s[pos] in '1'..'9') {
                while (pos < s.length && s[pos].isDigit()) pos++
            } else {
                throw JsonInvalido("Número no válido", pos)
            }
            if (consumir('.')) {
                if (fin || !s[pos].isDigit()) throw JsonInvalido("Faltan decimales", pos)
                while (pos < s.length && s[pos].isDigit()) pos++
            }
            if (pos < s.length && (s[pos] == 'e' || s[pos] == 'E')) {
                pos++
                if (pos < s.length && (s[pos] == '+' || s[pos] == '-')) pos++
                if (fin || !s[pos].isDigit()) throw JsonInvalido("Exponente no válido", pos)
                while (pos < s.length && s[pos].isDigit()) pos++
            }
            return Json.Numero(s.substring(inicio, pos).toDouble())
        }

        private fun literal(palabra: String, valor: Json): Json {
            if (!s.startsWith(palabra, pos)) throw JsonInvalido("Se esperaba '$palabra'", pos)
            pos += palabra.length
            return valor
        }

        private fun consumir(c: Char): Boolean {
            if (pos < s.length && s[pos] == c) {
                pos++
                return true
            }
            return false
        }
    }
}
