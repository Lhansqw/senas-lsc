package com.lhansqw.senaslsc.interpreter.litertlm

/** Modelo de lenguaje que la app sabe instalar. */
data class DescriptorModelo(
    val id: String,
    val nombre: String,
    /** Nombre del archivo en el almacenamiento privado de la app. */
    val archivo: String,
    /** Descarga directa (Hugging Face, comunidad LiteRT). */
    val url: String,
    /** Tamaño publicado; se usa para avisar antes de descargar y comprobar el espacio. */
    val tamanoAproximado: Long,
    /** Por debajo de este tamaño el archivo no puede ser el modelo (descarga cortada, archivo equivocado). */
    val tamanoMinimo: Long,
    val licencia: String,
    val paginaModelo: String,
) {
    init {
        require(url.startsWith("https://")) { "La descarga debe ser por HTTPS" }
        require(archivo.endsWith(EXTENSION)) { "El archivo debe tener la extensión $EXTENSION" }
        require(tamanoMinimo in 1..tamanoAproximado) { "Tamaños incoherentes" }
    }

    fun esArchivoPlausible(bytes: Long): Boolean = bytes >= tamanoMinimo

    companion object {
        const val EXTENSION = ".litertlm"
    }
}

object ModelosLenguaje {

    private const val MB = 1024L * 1024

    /**
     * Gemma 4 E2B (instrucciones) para LiteRT-LM. Licencia Apache 2.0.
     *
     * Según su ficha: unos 2,6 GB. En un Samsung S26 Ultra, por CPU, genera ~47 tokens/s y usa
     * ~1,7 GB de RAM. Los requisitos reales en otros teléfonos se miden en la fase 11.
     */
    val GEMMA_4_E2B = DescriptorModelo(
        id = "gemma-4-e2b-it",
        nombre = "Gemma 4 E2B",
        archivo = "gemma-4-E2B-it.litertlm",
        url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        tamanoAproximado = 2_583 * MB,
        tamanoMinimo = 1_024 * MB,
        licencia = "Apache 2.0",
        paginaModelo = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm",
    )

    val POR_DEFECTO = GEMMA_4_E2B
}
