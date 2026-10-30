package com.lhansqw.senaslsc.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.lhansqw.senaslsc.R

/** Copiar y compartir texto con las API estándar de Android. */
object AccionesTexto {

    fun copiar(context: Context, texto: String) {
        val portapapeles = context.getSystemService(ClipboardManager::class.java) ?: return
        portapapeles.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), texto))
        // Desde Android 13 el sistema ya muestra su propio aviso al copiar.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.accion_copiado, Toast.LENGTH_SHORT).show()
        }
    }

    fun compartir(context: Context, texto: String) {
        val enviar = Intent(Intent.ACTION_SEND).apply {
            type = ClipDescription.MIMETYPE_TEXT_PLAIN
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        val selector = Intent.createChooser(enviar, context.getString(R.string.accion_compartir))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(selector)
    }
}
