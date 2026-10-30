package com.lhansqw.senaslsc.ia

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** ¿Hay una red con salida a internet comprobada? (Para no intentar Gemini sin conexión.) */
object Conectividad {
    fun hayInternet(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capacidades = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
