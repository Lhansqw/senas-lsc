// Contrato del intérprete de texto, plantilla de instrucciones y validador de respuestas.
// Kotlin Multiplatform: compartido con la app de iPhone.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
    }
}
