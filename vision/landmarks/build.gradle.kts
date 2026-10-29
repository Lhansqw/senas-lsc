// Extracción de puntos de referencia con MediaPipe Holistic Landmarker.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.lhansqw.senaslsc.vision"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core:model"))
    implementation(libs.mediapipe.tasks.vision)

    testImplementation(libs.junit)
}

/**
 * Descarga el modelo de MediaPipe a los assets si no está.
 *
 * El modelo no se guarda en git (es un binario de varios MB): se descarga una vez de la URL
 * oficial de Google. La tarea comprueba que el archivo es un paquete .task válido (un ZIP).
 */
abstract class DescargarModelo : DefaultTask() {
    @get:Input
    abstract val url: Property<String>

    @get:OutputFile
    abstract val destino: RegularFileProperty

    @TaskAction
    fun descargar() {
        val archivo = destino.get().asFile
        archivo.parentFile.mkdirs()
        val temporal = File(archivo.parentFile, archivo.name + ".descargando")
        logger.lifecycle("Descargando ${url.get()}")
        java.net.URI(url.get()).toURL().openStream().use { entrada ->
            temporal.outputStream().use { salida -> entrada.copyTo(salida) }
        }
        val cabecera = temporal.inputStream().use { it.readNBytes(2) }
        if (temporal.length() < 1_000_000 || !cabecera.contentEquals(byteArrayOf(0x50, 0x4B))) {
            temporal.delete()
            throw GradleException("El archivo descargado no es un modelo .task válido: ${url.get()}")
        }
        if (!temporal.renameTo(archivo)) {
            temporal.delete()
            throw GradleException("No se pudo guardar el modelo en ${archivo.absolutePath}")
        }
        logger.lifecycle("Modelo guardado en ${archivo.absolutePath} (${archivo.length() / 1024} KB)")
    }
}

val descargarModeloHolistic = tasks.register<DescargarModelo>("descargarModeloHolistic") {
    group = "senas"
    description = "Descarga holistic_landmarker.task (MediaPipe) a src/main/assets si falta."
    url.set("https://storage.googleapis.com/mediapipe-models/holistic_landmarker/holistic_landmarker/float16/1/holistic_landmarker.task")
    destino.set(layout.projectDirectory.file("src/main/assets/holistic_landmarker.task"))
    // Si el archivo ya existe no se vuelve a descargar.
    outputs.upToDateWhen { destino.get().asFile.exists() }
}

tasks.named("preBuild") {
    dependsOn(descargarModeloHolistic)
}
