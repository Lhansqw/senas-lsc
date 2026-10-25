package com.lhansqw.senaslsc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lhansqw.senaslsc.dispositivo.EvaluadorCapacidad
import com.lhansqw.senaslsc.dispositivo.LectorPerfilDispositivo
import com.lhansqw.senaslsc.ui.PantallaInicio
import com.lhansqw.senaslsc.ui.theme.TemaSenasLSC

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val perfil = LectorPerfilDispositivo.leer(this)
        val capacidad = EvaluadorCapacidad.evaluar(perfil)

        setContent {
            TemaSenasLSC {
                PantallaInicio(perfil = perfil, capacidad = capacidad)
            }
        }
    }
}
