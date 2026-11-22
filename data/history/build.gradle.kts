// Historial de frases: modelo, formato y reglas. Kotlin Multiplatform: el repositorio y el formato
// están en commonMain; el cifrado y el archivo los aporta cada plataforma (en jvmMain, AES-GCM con
// la clave del Android Keystore y un archivo escrito de forma atómica).
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
        implementation(libs.kotlinx.datetime)
    }
}
