package com.lhansqw.senaslsc.vocabulario

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.recognizer.ContratoModelo
import com.lhansqw.senaslsc.recognizer.ResultadoContrato
import com.lhansqw.senaslsc.recognizer.litert.CargadorReconocedor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface EstadoVocabulario {
    data object Cargando : EstadoVocabulario
    data object SinModelo : EstadoVocabulario
    data class Invalido(val errores: List<String>) : EstadoVocabulario
    data class Listo(val contrato: ContratoModelo) : EstadoVocabulario
}

/** Pantalla «Vocabulario compatible»: las señas exactas que el modelo instalado reconoce. */
class VocabularioViewModel(application: Application) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow<EstadoVocabulario>(EstadoVocabulario.Cargando)
    val estado: StateFlow<EstadoVocabulario> = _estado.asStateFlow()

    private val _consulta = MutableStateFlow("")
    val consulta: StateFlow<String> = _consulta.asStateFlow()

    private val _tipo = MutableStateFlow<TipoSena?>(null)
    val tipo: StateFlow<TipoSena?> = _tipo.asStateFlow()

    val grupos: StateFlow<List<GrupoVocabulario>> = combine(_estado, _consulta, _tipo) { e, q, t ->
        if (e is EstadoVocabulario.Listo) filtrarVocabulario(e.contrato.entradas, q, t) else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _estado.value = withContext(Dispatchers.IO) {
                when (val r = CargadorReconocedor.leerContrato(getApplication())) {
                    null -> EstadoVocabulario.SinModelo
                    is ResultadoContrato.Invalido -> EstadoVocabulario.Invalido(r.errores)
                    is ResultadoContrato.Valido -> EstadoVocabulario.Listo(r.contrato)
                }
            }
        }
    }

    fun buscar(texto: String) {
        _consulta.value = texto
    }

    fun filtrarTipo(tipo: TipoSena?) {
        _tipo.value = tipo
    }
}
