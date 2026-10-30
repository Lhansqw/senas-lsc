package com.lhansqw.senaslsc.ia

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lhansqw.senaslsc.SenasApp
import com.lhansqw.senaslsc.dispositivo.CapacidadDispositivo
import com.lhansqw.senaslsc.interpreter.litertlm.DescriptorModelo
import com.lhansqw.senaslsc.interpreter.litertlm.EstadoModeloLenguaje
import com.lhansqw.senaslsc.interpreter.PreferenciaInterprete
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Pantalla de la IA: modelo de lenguaje del teléfono e IA de internet (Gemini, fase 10). */
class IaViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SenasApp
    private val gestor = app.gestorModelo

    val descriptor: DescriptorModelo = gestor.descriptor
    val capacidad: CapacidadDispositivo = app.capacidad
    val estadoModelo: StateFlow<EstadoModeloLenguaje> = gestor.estado
    val usarIaLocal: StateFlow<Boolean> = app.preferenciasIa.usarIaLocal

    init {
        gestor.refrescar()
        // Al terminar una instalación, la IA se activa: el usuario acaba de pedir el modelo.
        viewModelScope.launch {
            var anterior: EstadoModeloLenguaje? = null
            estadoModelo.collect { actual ->
                val instalando = anterior is EstadoModeloLenguaje.Descargando || anterior is EstadoModeloLenguaje.Importando
                if (instalando && actual is EstadoModeloLenguaje.Instalado) app.preferenciasIa.establecerUsarIaLocal(true)
                anterior = actual
            }
        }
    }

    fun descargar() {
        if (capacidad.interpreteLocal) gestor.descargar()
    }

    fun importar(uri: Uri) {
        if (capacidad.interpreteLocal) gestor.importar(uri)
    }

    fun cancelar() = gestor.cancelar()

    fun eliminar() {
        gestor.eliminar()
        app.preferenciasIa.establecerUsarIaLocal(false)
    }

    fun establecerUsarIaLocal(valor: Boolean) = app.preferenciasIa.establecerUsarIaLocal(valor)

    // --- IA de internet (Gemini) ---

    private val remotas = app.preferenciasIaRemota
    private val controladorRemoto = ControladorIaRemota(remotas)

    val consentimiento: StateFlow<Boolean> = remotas.consentimiento
    val hayClave: StateFlow<Boolean> = remotas.hayClave
    val modeloGemini: StateFlow<String> = remotas.modelo
    val preferencia: StateFlow<PreferenciaInterprete> = remotas.preferencia

    private val _prueba = MutableStateFlow<EstadoPrueba>(EstadoPrueba.Ninguna)
    val prueba: StateFlow<EstadoPrueba> = _prueba.asStateFlow()

    fun establecerConsentimiento(valor: Boolean) {
        remotas.establecerConsentimiento(valor)
        _prueba.value = EstadoPrueba.Ninguna
    }

    fun guardarClave(clave: String) {
        if (clave.isBlank()) return
        remotas.guardarClave(clave)
        probar()
    }

    fun borrarClave() {
        remotas.borrarClave()
        _prueba.value = EstadoPrueba.Ninguna
    }

    fun establecerModelo(modelo: String) {
        remotas.establecerModelo(modelo)
        _prueba.value = EstadoPrueba.Ninguna
    }

    fun establecerPreferencia(p: PreferenciaInterprete) = remotas.establecerPreferencia(p)

    /** Envía «HOLA YO» a Gemini para comprobar la clave y el modelo. Solo cuando el usuario lo pide. */
    fun probar() {
        if (_prueba.value == EstadoPrueba.Probando) return
        _prueba.value = EstadoPrueba.Probando
        viewModelScope.launch {
            _prueba.value = controladorRemoto.probar()?.let { EstadoPrueba.Fallida(it) } ?: EstadoPrueba.Correcta
        }
    }
}
