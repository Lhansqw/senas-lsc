// Archivo de construcción raíz: declara los plugins sin aplicarlos.
// Cada módulo aplica los que necesita.

buildscript {
    dependencies {
        // AGP 9 incluye Kotlin integrado. Esta línea fija una versión más reciente de KGP,
        // que debe coincidir con la del plugin del compilador de Compose.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
