package com.lhansqw.senaslsc.recognizer.litert

import android.content.Context
import android.util.Log
import com.lhansqw.senaslsc.recognizer.ContratoModelo
import com.lhansqw.senaslsc.recognizer.LectorContrato
import com.lhansqw.senaslsc.recognizer.ResultadoContrato
import java.io.IOException

sealed interface ResultadoCarga {
    data class Cargado(val reconocedor: ReconocedorLiteRT, val contrato: ContratoModelo) : ResultadoCarga

    /** Todavía no se ha entrenado ni copiado ningún modelo (fase 6). */
    data object SinModelo : ResultadoCarga

    /** Hay modelo, pero modelo.json no es válido o no es compatible con la app. */
    data class Invalido(val errores: List<String>) : ResultadoCarga

    /** El modelo es compatible pero LiteRT no pudo cargarlo o ejecutarlo. */
    data class Fallo(val mensaje: String) : ResultadoCarga
}

/** Busca el modelo y su descripción en los assets y los carga. */
object CargadorReconocedor {

    private const val TAG = "CargadorReconocedor"

    /**
     * Lee solo la descripción del modelo (sin cargarlo en LiteRT), para mostrar su vocabulario.
     * Devuelve `null` si no hay ningún modelo instalado. Hace E/S: llamar fuera del hilo principal.
     */
    fun leerContrato(context: Context): ResultadoContrato? {
        val assets = context.applicationContext.assets
        val disponibles = try {
            assets.list("")?.toSet().orEmpty()
        } catch (e: IOException) {
            return ResultadoContrato.Invalido(listOf("No se pudieron listar los assets: ${e.message}"))
        }
        if (ReconocedorLiteRT.ARCHIVO_CONTRATO !in disponibles) return null
        return try {
            LectorContrato.leer(assets.open(ReconocedorLiteRT.ARCHIVO_CONTRATO).bufferedReader(Charsets.UTF_8).use { it.readText() })
        } catch (e: IOException) {
            ResultadoContrato.Invalido(listOf("No se pudo leer ${ReconocedorLiteRT.ARCHIVO_CONTRATO}: ${e.message}"))
        }
    }

    /** Debe llamarse desde el hilo de fondo donde se usará el reconocedor. */
    fun cargar(context: Context): ResultadoCarga {
        val assets = context.applicationContext.assets
        val disponibles = try {
            assets.list("")?.toSet().orEmpty()
        } catch (e: IOException) {
            return ResultadoCarga.Fallo("No se pudieron listar los assets: ${e.message}")
        }
        val hayModelo = ReconocedorLiteRT.ARCHIVO_MODELO in disponibles
        val hayContrato = ReconocedorLiteRT.ARCHIVO_CONTRATO in disponibles
        when {
            !hayModelo && !hayContrato -> return ResultadoCarga.SinModelo
            !hayModelo -> return ResultadoCarga.Invalido(listOf("Falta ${ReconocedorLiteRT.ARCHIVO_MODELO} junto a ${ReconocedorLiteRT.ARCHIVO_CONTRATO}."))
            !hayContrato -> return ResultadoCarga.Invalido(listOf("Falta ${ReconocedorLiteRT.ARCHIVO_CONTRATO} junto a ${ReconocedorLiteRT.ARCHIVO_MODELO}."))
        }

        val texto = try {
            assets.open(ReconocedorLiteRT.ARCHIVO_CONTRATO).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IOException) {
            return ResultadoCarga.Fallo("No se pudo leer ${ReconocedorLiteRT.ARCHIVO_CONTRATO}: ${e.message}")
        }

        val contrato = when (val r = LectorContrato.leer(texto)) {
            is ResultadoContrato.Invalido -> return ResultadoCarga.Invalido(r.errores)
            is ResultadoContrato.Valido -> r.contrato
        }

        return try {
            ResultadoCarga.Cargado(ReconocedorLiteRT.crear(context, contrato), contrato)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo cargar el modelo de señas", e)
            ResultadoCarga.Fallo("LiteRT no pudo cargar el modelo: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
