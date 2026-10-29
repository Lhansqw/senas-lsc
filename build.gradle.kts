// Archivo de construcción raíz: declara los plugins sin aplicarlos.
// Cada módulo aplica los que necesita.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

buildscript {
    dependencies {
        // AGP 9 incluye Kotlin integrado. Esta línea fija una versión más reciente de KGP,
        // que debe coincidir con la del plugin del compilador de Compose.
        // Los módulos Kotlin puros aplican org.jetbrains.kotlin.jvm sin versión: la toman de aquí.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

subprojects {
    // Grupo distinto por carpeta: evita que :recognizer:api e :interpreter:api,
    // que comparten el nombre "api", se confundan al resolver dependencias.
    group = "com.lhansqw.senaslsc" + (parent?.path ?: ":").replace(':', '.').trimEnd('.')

    // Configuración común de los módulos Kotlin puros (sin Android).
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        }
        dependencies {
            "testImplementation"(libs.junit)
        }
    }
}
