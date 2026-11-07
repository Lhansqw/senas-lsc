pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Los repositorios se declaran solo aquí; los módulos no pueden añadir otros.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SenasLSC"

include(":app")

// Módulos Kotlin puros: lógica y contratos, probados con JUnit sin emulador. Los que aplican
// org.jetbrains.kotlin.multiplatform se comparten también con la app de iPhone (ios/).
include(":core:model")
include(":core:descargas")
include(":recognizer:api")
include(":sequence")
include(":interpreter:api")
include(":interpreter:rules")
include(":interpreter:remoto")
include(":data:history")
include(":vision:comun")

// Lógica de reconocimiento compartida y XCFramework para la app de iPhone (ios/).
include(":shared")

// Módulos Android.
include(":camera")
include(":vision:landmarks")
include(":recognizer:litert")
include(":interpreter:litertlm")

// Módulos Android que se añadirán en sus fases:
// :interpreter:remote (10).
