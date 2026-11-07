// Archivo de construcción raíz: declara los plugins sin aplicarlos.
// Cada módulo aplica los que necesita.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

buildscript {
    dependencies {
        // AGP 9 incluye Kotlin integrado. Esta línea fija una versión más reciente de KGP,
        // que debe coincidir con la del plugin del compilador de Compose.
        // Los módulos Kotlin puros aplican org.jetbrains.kotlin.jvm o .multiplatform sin versión:
        // la toman de aquí.
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

    // Módulos Kotlin Multiplatform: lógica compartida con la app de iPhone (y Apple Watch).
    // Android y las pruebas JUnit usan el target jvm(). Los de iOS solo se compilan del todo
    // en una Mac; en Windows, Gradle los omite.
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            jvm {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
            }
            iosArm64()
            iosSimulatorArm64()

            sourceSets.getByName("jvmTest").dependencies {
                implementation(libs.junit)
            }
        }
        // Así `gradlew test` sigue ejecutando también las pruebas de estos módulos.
        tasks.register("test") {
            group = "verification"
            description = "Ejecuta las pruebas JUnit del target jvm."
            dependsOn("jvmTest")
        }
    }
}
