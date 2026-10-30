package com.lhansqw.senaslsc

import android.app.Application
import com.lhansqw.senaslsc.dispositivo.CapacidadDispositivo
import com.lhansqw.senaslsc.dispositivo.EvaluadorCapacidad
import com.lhansqw.senaslsc.dispositivo.LectorPerfilDispositivo
import com.lhansqw.senaslsc.historial.PreferenciasHistorial
import com.lhansqw.senaslsc.historial.ServicioHistorial
import com.lhansqw.senaslsc.ia.PreferenciasIa
import com.lhansqw.senaslsc.ia.PreferenciasIaRemota
import com.lhansqw.senaslsc.interpreter.litertlm.GestorModeloLenguaje
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Objetos que viven lo mismo que la app. El gestor del modelo de lenguaje está aquí para que una
 * descarga de 2,6 GB no se corte al cambiar de pantalla.
 */
class SenasApp : Application() {

    /** Ámbito de corrutinas de la app: no se cancela al cerrar una pantalla. */
    val ambito: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val capacidad: CapacidadDispositivo by lazy { EvaluadorCapacidad.evaluar(LectorPerfilDispositivo.leer(this)) }
    val preferenciasIa: PreferenciasIa by lazy { PreferenciasIa(this) }
    val preferenciasIaRemota: PreferenciasIaRemota by lazy { PreferenciasIaRemota(this) }
    val gestorModelo: GestorModeloLenguaje by lazy { GestorModeloLenguaje(this, ambito) }
    val preferenciasHistorial: PreferenciasHistorial by lazy { PreferenciasHistorial(this) }
    val historial: ServicioHistorial by lazy { ServicioHistorial(this, ambito).also { it.cargar() } }
}
