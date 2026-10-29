package com.lhansqw.senaslsc.ia

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lhansqw.senaslsc.SenasApp
import com.lhansqw.senaslsc.dispositivo.CapacidadDispositivo
import com.lhansqw.senaslsc.interpreter.litertlm.DescriptorModelo
import com.lhansqw.senaslsc.interpreter.litertlm.EstadoModeloLenguaje
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Pantalla de la IA: instalación del modelo de lenguaje y preferencia de uso. */
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
}
