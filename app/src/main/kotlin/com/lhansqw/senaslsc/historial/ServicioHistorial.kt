package com.lhansqw.senaslsc.historial

import android.content.Context
import com.lhansqw.senaslsc.data.history.AlmacenArchivo
import com.lhansqw.senaslsc.data.history.CifradorAesGcm
import com.lhansqw.senaslsc.data.history.EntradaHistorial
import com.lhansqw.senaslsc.data.history.OrigenTexto
import com.lhansqw.senaslsc.data.history.RepositorioHistorial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Estado del historial para las pantallas. */
data class EstadoHistorial(
    val entradas: List<EntradaHistorial> = emptyList(),
    val cargado: Boolean = false,
    /** No se pudo leer el historial anterior (p. ej. tras reinstalar) y se empezó de cero. */
    val recuperadoDeError: Boolean = false,
)

/**
 * Acceso al historial desde la app. Todas las operaciones van a un único hilo de entrada/salida
 * (paralelismo 1), así el repositorio nunca se usa desde dos hilos a la vez.
 *
 * El archivo está en el almacenamiento privado y, además, cifrado con una clave del Keystore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServicioHistorial(context: Context, private val ambito: CoroutineScope) {

    private val repositorio = RepositorioHistorial(
        almacen = AlmacenArchivo(File(context.applicationContext.filesDir, "historial/historial.bin")),
        cifrador = CifradorAesGcm { ClaveKeystore.obtener() },
    )
    private val despachador = Dispatchers.IO.limitedParallelism(1)

    private val _estado = MutableStateFlow(EstadoHistorial())
    val estado: StateFlow<EstadoHistorial> = _estado.asStateFlow()

    fun cargar() = ejecutar { }

    fun guardar(texto: String, glosas: List<String>, origen: OrigenTexto) = ejecutar {
        repositorio.agregar(texto, glosas, origen)
    }

    fun eliminar(id: String) = ejecutar { repositorio.eliminar(id) }

    fun vaciar() = ejecutar { repositorio.vaciar() }

    private fun ejecutar(accion: () -> Unit) {
        ambito.launch(despachador) {
            accion()
            _estado.value = EstadoHistorial(repositorio.listar(), cargado = true, recuperadoDeError = repositorio.recuperadoDeError)
        }
    }
}
