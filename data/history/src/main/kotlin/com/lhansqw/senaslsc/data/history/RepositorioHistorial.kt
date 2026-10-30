package com.lhansqw.senaslsc.data.history

import com.lhansqw.senaslsc.core.texto.coincideBusqueda

/** Cifra y descifra bloques de bytes. En Android lo implementa una clave del Android Keystore. */
interface Cifrador {
    fun cifrar(datos: ByteArray): ByteArray

    /** Lanza una excepción si los datos están dañados o se cifraron con otra clave. */
    fun descifrar(datos: ByteArray): ByteArray
}

/** Dónde se guardan los bytes (ya cifrados). En Android, un archivo privado escrito de forma atómica. */
interface Almacen {
    /** `null` si todavía no hay nada guardado. */
    fun leer(): ByteArray?
    fun escribir(datos: ByteArray)
    fun borrar()
}

/**
 * Historial de frases, cifrado en reposo.
 *
 * - Se carga la primera vez que se usa. Si no se puede descifrar o leer (por ejemplo, porque se
 *   reinstaló la app y la clave del Keystore ya no existe), se empieza vacío y [recuperadoDeError]
 *   lo indica para avisar al usuario; los datos ilegibles se borran en la siguiente escritura.
 * - Guarda como máximo [maximo] entradas; al superarlo se eliminan las más antiguas.
 * - No es seguro para varios hilos: se usa desde un único hilo de entrada/salida.
 */
class RepositorioHistorial(
    private val almacen: Almacen,
    private val cifrador: Cifrador,
    private val maximo: Int = 500,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val nuevoId: () -> String = { java.util.UUID.randomUUID().toString() },
) {
    init {
        require(maximo > 0) { "El máximo debe ser positivo" }
    }

    private var entradas: MutableList<EntradaHistorial>? = null

    var recuperadoDeError: Boolean = false
        private set

    /** Entradas de la más reciente a la más antigua. */
    fun listar(): List<EntradaHistorial> = cargadas().sortedByDescending { it.creadaMs }

    fun buscar(consulta: String): List<EntradaHistorial> =
        listar().filter { e -> coincideBusqueda(e.texto, consulta) || e.glosas.any { coincideBusqueda(it, consulta) } }

    fun agregar(texto: String, glosas: List<String>, origen: OrigenTexto): EntradaHistorial {
        val entrada = EntradaHistorial(nuevoId(), reloj(), texto.trim(), glosas, origen)
        val lista = cargadas()
        lista += entrada
        if (lista.size > maximo) {
            lista.sortBy { it.creadaMs }
            repeat(lista.size - maximo) { lista.removeAt(0) }
        }
        guardar()
        return entrada
    }

    fun eliminar(id: String): Boolean {
        val quitada = cargadas().removeAll { it.id == id }
        if (quitada) guardar()
        return quitada
    }

    /** Borra todo el historial, también del almacenamiento. */
    fun vaciar() {
        entradas = mutableListOf()
        almacen.borrar()
        recuperadoDeError = false
    }

    private fun cargadas(): MutableList<EntradaHistorial> {
        entradas?.let { return it }
        val cargada = try {
            almacen.leer()?.let { FormatoHistorial.leer(cifrador.descifrar(it)) }.orEmpty().toMutableList()
        } catch (e: Exception) {
            recuperadoDeError = true
            mutableListOf()
        }
        entradas = cargada
        return cargada
    }

    private fun guardar() {
        almacen.escribir(cifrador.cifrar(FormatoHistorial.escribir(cargadas())))
    }
}
