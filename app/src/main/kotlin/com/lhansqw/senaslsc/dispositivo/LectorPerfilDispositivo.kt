package com.lhansqw.senaslsc.dispositivo

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/** Lee del sistema los datos que necesita [EvaluadorCapacidad]. */
object LectorPerfilDispositivo {

    fun leer(context: Context): PerfilDispositivo {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoria = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }

        return PerfilDispositivo(
            nivelApi = Build.VERSION.SDK_INT,
            ramTotalMb = memoria.totalMem / (1024 * 1024),
            esDispositivoBajaMemoria = activityManager.isLowRamDevice,
            nucleos = Runtime.getRuntime().availableProcessors(),
        )
    }
}
