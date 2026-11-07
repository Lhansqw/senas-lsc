// Contrato del reconocedor visual y cálculo de características. Kotlin Multiplatform:
// Android lo implementa con LiteRT y iPhone con TensorFlow Lite para iOS.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
    }
}
