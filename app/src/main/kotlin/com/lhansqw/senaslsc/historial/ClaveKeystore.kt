package com.lhansqw.senaslsc.historial

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Clave AES-256 del historial guardada en el **Android Keystore**: se genera en el teléfono, no se
 * puede exportar y, en los dispositivos con hardware seguro, nunca sale de él.
 *
 * Si se desinstala la app, la clave desaparece y el historial anterior queda ilegible; el
 * repositorio lo detecta y empieza vacío.
 */
object ClaveKeystore {

    private const val PROVEEDOR = "AndroidKeyStore"
    private const val ALIAS = "senas_lsc_historial_v1"

    @Synchronized
    fun obtener(): SecretKey {
        val almacen = KeyStore.getInstance(PROVEEDOR).apply { load(null) }
        (almacen.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // El IV lo genera el Keystore en cada cifrado; no se permite reutilizar uno propio.
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generador.generateKey()
    }
}
