package com.lhansqw.senaslsc.historial

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lhansqw.senaslsc.SenasApp
import com.lhansqw.senaslsc.core.texto.coincideBusqueda
import com.lhansqw.senaslsc.data.history.EntradaHistorial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Pantalla del historial: lista, búsqueda, borrado y preferencia de guardar. */
class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SenasApp
    private val servicio = app.historial

    val guardar: StateFlow<Boolean> = app.preferenciasHistorial.guardar

    private val _consulta = MutableStateFlow("")
    val consulta: StateFlow<String> = _consulta.asStateFlow()

    val estado: StateFlow<EstadoHistorial> = servicio.estado

    /** Entradas que coinciden con la búsqueda (texto o glosas, sin tildes). */
    val visibles: StateFlow<List<EntradaHistorial>> = combine(servicio.estado, _consulta) { e, q ->
        e.entradas.filter { entrada -> coincideBusqueda(entrada.texto, q) || entrada.glosas.any { coincideBusqueda(it, q) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        servicio.cargar()
    }

    fun buscar(texto: String) {
        _consulta.value = texto
    }

    fun establecerGuardar(valor: Boolean) = app.preferenciasHistorial.establecerGuardar(valor)

    fun eliminar(id: String) = servicio.eliminar(id)

    fun vaciar() = servicio.vaciar()
}
