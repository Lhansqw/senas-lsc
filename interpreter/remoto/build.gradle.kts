// Intérprete remoto (fase 10): cliente de la API de Gemini y comparativa. Kotlin Multiplatform:
// la petición y la lectura de la respuesta (ProtocoloGemini) están en commonMain; la conexión HTTP
// (HttpURLConnection) y la comparativa solo existen en el target jvm, que es el que usa Android.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":interpreter:api"))
        implementation(project(":interpreter:rules"))
    }
}

// Comparativa reglas frente a Gemini con secuencias de prueba (fase 10):
//   set GEMINI_API_KEY=...   (en PowerShell: $env:GEMINI_API_KEY="...")
//   gradlew.bat :interpreter:remoto:compararGemini
// La clave se lee de la variable de entorno; nunca se escribe en archivos ni en la salida.
tasks.register<JavaExec>("compararGemini") {
    group = "verification"
    description = "Compara el intérprete por reglas con Gemini: calidad, latencia y coste."
    val principal = kotlin.jvm().compilations.getByName("main")
    classpath = files(principal.output.allOutputs, principal.runtimeDependencyFiles)
    mainClass.set("com.lhansqw.senaslsc.interpreter.remoto.ComparativaKt")
    args(layout.buildDirectory.file("comparativa/comparativa-gemini.md").get().asFile.absolutePath)
    providers.gradleProperty("modeloGemini").orNull?.let { args(it) }
}
