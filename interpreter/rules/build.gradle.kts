// Intérprete determinista: siempre disponible, sin modelo de lenguaje y sin Internet.
// Kotlin Multiplatform: es el intérprete de la primera versión de iPhone.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":interpreter:api"))
    }
}
