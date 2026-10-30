package com.lhansqw.senaslsc.historial

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Si se guardan las frases en el historial. Desactivado por defecto: el historial es opcional. */
class PreferenciasHistorial(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("preferencias_historial", Context.MODE_PRIVATE)
    private val _guardar = MutableStateFlow(prefs.getBoolean(CLAVE, false))
    val guardar: StateFlow<Boolean> = _guardar.asStateFlow()

    fun establecerGuardar(valor: Boolean) {
        prefs.edit().putBoolean(CLAVE, valor).apply()
        _guardar.value = valor
    }

    private companion object {
        const val CLAVE = "guardar_historial"
    }
}
