package com.lhansqw.senaslsc.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ModoTema { SISTEMA, CLARO, OSCURO }

/** Tema de las pantallas de menú. Por defecto sigue al sistema hasta que la persona elige uno. */
class PreferenciasTema(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("preferencias_tema", Context.MODE_PRIVATE)
    private val _modo = MutableStateFlow(
        prefs.getString(CLAVE, null)?.let { guardado -> ModoTema.entries.firstOrNull { it.name == guardado } } ?: ModoTema.SISTEMA,
    )
    val modo: StateFlow<ModoTema> = _modo.asStateFlow()

    fun establecer(modo: ModoTema) {
        prefs.edit().putString(CLAVE, modo.name).apply()
        _modo.value = modo
    }

    private companion object {
        const val CLAVE = "modo_tema"
    }
}
