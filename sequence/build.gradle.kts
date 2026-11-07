// Ventanas de fotogramas, filtro temporal y construcción de la secuencia de señas.
// Kotlin Multiplatform: compartido con la app de iPhone.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
    }
}
