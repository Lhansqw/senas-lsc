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

// Módulos Kotlin puros: lógica y contratos, probados con JUnit sin emulador.
include(":core:model")
include(":recognizer:api")
include(":sequence")
include(":interpreter:api")
include(":interpreter:rules")

// Módulos Android.
include(":camera")
include(":vision:landmarks")
include(":recognizer:litert")

// Módulos Android que se añadirán en sus fases:
// :interpreter:litertlm (8), :data:history (9), :interpreter:remote (10).
