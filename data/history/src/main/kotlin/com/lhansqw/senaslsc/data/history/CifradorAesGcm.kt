package com.lhansqw.senaslsc.data.history

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifrado autenticado AES-GCM. Cada escritura usa un vector de inicialización nuevo, generado por
 * el proveedor criptográfico, y GCM detecta cualquier modificación de los datos al descifrar.
 *
 * Formato del bloque cifrado («sobre»): `[versión=1][longitud IV][IV][texto cifrado + etiqueta]`.
 *
 * @param clave proveedor de la clave. En Android, una clave AES-256 del Android Keystore que nunca
 *   sale del hardware seguro; en las pruebas, una clave en memoria.
 */
class CifradorAesGcm(private val clave: () -> SecretKey) : Cifrador {

    override fun cifrar(datos: ByteArray): ByteArray {
        val cifra = Cipher.getInstance(TRANSFORMACION)
        cifra.init(Cipher.ENCRYPT_MODE, clave())
        val iv = cifra.iv
        val cifrado = cifra.doFinal(datos)
        return ByteBuffer.allocate(2 + iv.size + cifrado.size)
            .put(VERSION)
            .put(iv.size.toByte())
            .put(iv)
            .put(cifrado)
            .array()
    }

    override fun descifrar(datos: ByteArray): ByteArray {
        if (datos.size < 2) throw GeneralSecurityException("Bloque cifrado demasiado corto")
        val buffer = ByteBuffer.wrap(datos)
        val version = buffer.get()
        if (version != VERSION) throw GeneralSecurityException("Versión de cifrado desconocida: $version")
        val longitudIv = buffer.get().toInt() and 0xFF
        if (longitudIv !in 12..16 || buffer.remaining() < longitudIv + BITS_ETIQUETA / 8) {
            throw GeneralSecurityException("Bloque cifrado mal formado")
        }
        val iv = ByteArray(longitudIv).also { buffer.get(it) }
        val cifrado = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val cifra = Cipher.getInstance(TRANSFORMACION)
        cifra.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(BITS_ETIQUETA, iv))
        return cifra.doFinal(cifrado)
    }

    private companion object {
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val VERSION: Byte = 1
        const val BITS_ETIQUETA = 128
    }
}
