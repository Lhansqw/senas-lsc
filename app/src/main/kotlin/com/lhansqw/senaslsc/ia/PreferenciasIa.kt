package com.lhansqw.senaslsc.ia

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferencias del usuario sobre la IA. Se guardan en el teléfono (SharedPreferences privadas) y
 * están excluidas de las copias de seguridad por las reglas de extracción de datos de la app.
 */
class PreferenciasIa(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    private val _usarIaLocal = MutableStateFlow(prefs.getBoolean(USAR_IA_LOCAL, false))

    /** Mejorar las frases con el modelo de lenguaje del teléfono. Desactivado hasta que el usuario lo elija. */
    val usarIaLocal: StateFlow<Boolean> = _usarIaLocal.asStateFlow()

    fun establecerUsarIaLocal(valor: Boolean) {
        prefs.edit().putBoolean(USAR_IA_LOCAL, valor).apply()
        _usarIaLocal.value = valor
    }

    private companion object {
        const val ARCHIVO = "preferencias_ia"
        const val USAR_IA_LOCAL = "usar_ia_local"
    }
}
