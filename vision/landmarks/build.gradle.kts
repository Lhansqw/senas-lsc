import java.io.RandomAccessFile
import java.net.URI

// Extracción de puntos de referencia con MediaPipe Hand Landmarker + Pose Landmarker.
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
    api(project(":vision:comun"))
    // ConstructorFotograma: qué mano es cuál con las muñecas de la pose (lo comparte con iOS).
    implementation(project(":shared"))
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
        URI(url.get()).toURL().openStream().use { entrada ->
            temporal.outputStream().use { salida -> entrada.copyTo(salida) }
        }
        if (temporal.length() < 1_000_000 || !esZip(temporal)) {
            temporal.delete()
            throw GradleException("El archivo descargado no es un modelo .task válido: ${url.get()}")
        }
        if (!temporal.renameTo(archivo)) {
            temporal.delete()
            throw GradleException("No se pudo guardar el modelo en ${archivo.absolutePath}")
        }
        logger.lifecycle("Modelo guardado en ${archivo.absolutePath} (${archivo.length() / 1024} KB)")
    }

    /**
     * Busca el final del directorio central del ZIP (`PK\u0005\u0006`) en los últimos 64 KB.
     * No basta con mirar el principio: los .task de MediaPipe llevan relleno antes del primer `PK`.
     */
    private fun esZip(archivo: File): Boolean {
        val cola = minOf(archivo.length(), 65_557L).toInt()
        val bytes = RandomAccessFile(archivo, "r").use { f ->
            f.seek(archivo.length() - cola)
            ByteArray(cola).also(f::readFully)
        }
        return (0..cola - 4).any {
            bytes[it] == 0x50.toByte() && bytes[it + 1] == 0x4B.toByte() && bytes[it + 2] == 0x05.toByte() && bytes[it + 3] == 0x06.toByte()
        }
    }
}

// Los mismos modelos que la app de iPhone (ios/preparar.sh).
val modelosMediaPipe = mapOf(
    "Manos" to "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task",
    "Pose" to "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/1/pose_landmarker_full.task",
)

val descargasModelos = modelosMediaPipe.map { (nombre, direccion) ->
    val archivo = direccion.substringAfterLast('/')
    tasks.register<DescargarModelo>("descargarModelo$nombre") {
        group = "senas"
        description = "Descarga $archivo (MediaPipe) a src/main/assets si falta."
        url.set(direccion)
        destino.set(layout.projectDirectory.file("src/main/assets/$archivo"))
        // Si el archivo ya existe no se vuelve a descargar.
        outputs.upToDateWhen { destino.get().asFile.exists() }
        onlyIf("el modelo aún no está en los assets") { !destino.get().asFile.exists() }
    }
}

tasks.named("preBuild") {
    dependsOn(descargasModelos)
}
