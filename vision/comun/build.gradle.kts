// Qué puntos de MediaPipe se conservan en cada fotograma (Subconjuntos). Kotlin Multiplatform:
// Android (Holistic) e iPhone (Hand + Pose) construyen los fotogramas con las mismas reglas.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
    }
}
