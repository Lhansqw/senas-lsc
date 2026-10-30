package com.lhansqw.senaslsc.data.history

/** Almacén en memoria para las pruebas. */
internal class AlmacenMemoria : Almacen {
    var datos: ByteArray? = null
    var escrituras = 0
    override fun leer() = datos?.copyOf()
    override fun escribir(datos: ByteArray) {
        this.datos = datos.copyOf()
        escrituras++
    }
    override fun borrar() {
        datos = null
    }
}

/** «Cifrado» reversible de prueba (XOR con una clave), para comprobar que los datos no van en claro. */
internal class CifradorPrueba(private val clave: Byte = 0x5A) : Cifrador {
    override fun cifrar(datos: ByteArray) = ByteArray(datos.size) { (datos[it].toInt() xor clave.toInt()).toByte() }
    override fun descifrar(datos: ByteArray) = cifrar(datos)
}

/** Simula una clave perdida: no puede descifrar nada. */
internal class CifradorRoto : Cifrador {
    override fun cifrar(datos: ByteArray) = datos
    override fun descifrar(datos: ByteArray): ByteArray = throw IllegalStateException("clave no disponible")
}
