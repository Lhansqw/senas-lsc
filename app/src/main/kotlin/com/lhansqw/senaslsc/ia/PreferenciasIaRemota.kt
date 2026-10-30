package com.lhansqw.senaslsc.ia

import android.content.Context
import android.util.Log
import com.lhansqw.senaslsc.data.history.CifradorAesGcm
import com.lhansqw.senaslsc.historial.ClaveKeystore
import com.lhansqw.senaslsc.interpreter.PreferenciaInterprete
import com.lhansqw.senaslsc.interpreter.remoto.ClienteGemini
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Base64

/**
 * Preferencias de la IA de internet (Gemini, fase 10).
 *
 * - **Consentimiento**: desactivado por defecto. Sin él, la app nunca envía nada a internet.
 * - **Clave de API**: cifrada con AES-GCM y una clave del Android Keystore que no sale del
 *   teléfono. Nunca se muestra entera, no se registra y está excluida de las copias de seguridad.
 */
class PreferenciasIaRemota(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
    private val cifrador = CifradorAesGcm { ClaveKeystore.obtener(ClaveKeystore.ALIAS_GEMINI) }

    private val _consentimiento = MutableStateFlow(prefs.getBoolean(CONSENTIMIENTO, false))
    val consentimiento: StateFlow<Boolean> = _consentimiento.asStateFlow()

    private val _hayClave = MutableStateFlow(prefs.contains(CLAVE))
    val hayClave: StateFlow<Boolean> = _hayClave.asStateFlow()

    private val _modelo = MutableStateFlow(prefs.getString(MODELO, null) ?: ClienteGemini.MODELO_PREDETERMINADO)
    val modelo: StateFlow<String> = _modelo.asStateFlow()

    private val _preferencia = MutableStateFlow(
        prefs.getString(PREFERENCIA, null)?.let { runCatching { PreferenciaInterprete.valueOf(it) }.getOrNull() }
            ?: PreferenciaInterprete.AUTOMATICO,
    )
    val preferencia: StateFlow<PreferenciaInterprete> = _preferencia.asStateFlow()

    /** Se puede usar Gemini: el usuario aceptó y hay una clave. */
    val permitido: Boolean get() = _consentimiento.value && _hayClave.value

    fun establecerConsentimiento(valor: Boolean) {
        prefs.edit().putBoolean(CONSENTIMIENTO, valor).apply()
        _consentimiento.value = valor
    }

    fun guardarClave(clave: String) {
        val limpia = clave.trim()
        require(limpia.isNotEmpty()) { "La clave está vacía" }
        val cifrada = cifrador.cifrar(limpia.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(CLAVE, Base64.getEncoder().encodeToString(cifrada)).apply()
        _hayClave.value = true
    }

    /** La clave descifrada, o `null` si no hay o ya no se puede leer (p. ej. tras reinstalar). */
    fun leerClave(): String? {
        val guardada = prefs.getString(CLAVE, null) ?: return null
        return try {
            cifrador.descifrar(Base64.getDecoder().decode(guardada)).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo descifrar la clave de Gemini; se borra")
            borrarClave()
            null
        }
    }

    fun borrarClave() {
        prefs.edit().remove(CLAVE).apply()
        _hayClave.value = false
    }

    fun establecerModelo(modelo: String) {
        val m = modelo.trim().ifEmpty { ClienteGemini.MODELO_PREDETERMINADO }
        prefs.edit().putString(MODELO, m).apply()
        _modelo.value = m
    }

    fun establecerPreferencia(p: PreferenciaInterprete) {
        prefs.edit().putString(PREFERENCIA, p.name).apply()
        _preferencia.value = p
    }

    private companion object {
        const val TAG = "PreferenciasIaRemota"
        const val ARCHIVO = "preferencias_ia_remota"
        const val CONSENTIMIENTO = "consentimiento"
        const val CLAVE = "clave_cifrada"
        const val MODELO = "modelo"
        const val PREFERENCIA = "preferencia"
    }
}
