package com.lhansqw.senaslsc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.lhansqw.senaslsc.camara.PantallaCamara
import com.lhansqw.senaslsc.dispositivo.EvaluadorCapacidad
import com.lhansqw.senaslsc.dispositivo.LectorPerfilDispositivo
import com.lhansqw.senaslsc.ui.PantallaInicio
import com.lhansqw.senaslsc.ui.theme.TemaSenasLSC

/** Pantallas de la app. La navegación es simple todavía: no hace falta una librería. */
private enum class Pantalla { INICIO, CAMARA }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val perfil = LectorPerfilDispositivo.leer(this)
        val capacidad = EvaluadorCapacidad.evaluar(perfil)

        setContent {
            var pantalla by rememberSaveable { mutableStateOf(Pantalla.INICIO) }

            TemaSenasLSC {
                when (pantalla) {
                    Pantalla.INICIO -> PantallaInicio(
                        perfil = perfil,
                        capacidad = capacidad,
                        onAbrirCamara = { pantalla = Pantalla.CAMARA },
                    )
                    Pantalla.CAMARA -> PantallaCamara(onVolver = { pantalla = Pantalla.INICIO })
                }
            }
        }
    }
}
