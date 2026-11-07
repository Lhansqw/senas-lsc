import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

// Lógica de reconocimiento compartida entre Android, iPhone y los relojes: coordinador, estado,
// fachada para Swift y resumen para el reloj.
//
// En iOS se usa como SenasShared.xcframework (solo se puede generar en una Mac):
//   ./gradlew :shared:assembleSenasSharedXCFramework
// El resultado queda en shared/build/XCFrameworks/{debug,release}/ y lo enlaza ios/project.yml.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    val xcf = XCFramework("SenasShared")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "SenasShared"
            isStatic = true
            // Swift ve directamente los tipos de estos módulos (LandmarkFrame, PropuestaFrase…).
            export(project(":core:model"))
            export(project(":recognizer:api"))
            export(project(":sequence"))
            export(project(":interpreter:api"))
            export(project(":interpreter:rules"))
            export(project(":vision:comun"))
            xcf.add(this)
        }
    }

    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
        api(project(":recognizer:api"))
        api(project(":sequence"))
        api(project(":interpreter:api"))
        api(project(":interpreter:rules"))
        api(project(":vision:comun"))
    }
}
