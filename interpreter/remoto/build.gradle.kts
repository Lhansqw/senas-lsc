// Intérprete remoto (fase 10): cliente de la API de Gemini y comparativa. Kotlin puro, sin Android.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    api(project(":interpreter:api"))
    implementation(project(":interpreter:rules"))
}

// Comparativa reglas frente a Gemini con secuencias de prueba (fase 10):
//   set GEMINI_API_KEY=...   (en PowerShell: $env:GEMINI_API_KEY="...")
//   gradlew.bat :interpreter:remoto:compararGemini
// La clave se lee de la variable de entorno; nunca se escribe en archivos ni en la salida.
tasks.register<JavaExec>("compararGemini") {
    group = "verification"
    description = "Compara el intérprete por reglas con Gemini: calidad, latencia y coste."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.lhansqw.senaslsc.interpreter.remoto.ComparativaKt")
    args(layout.buildDirectory.file("comparativa/comparativa-gemini.md").get().asFile.absolutePath)
    providers.gradleProperty("modeloGemini").orNull?.let { args(it) }
}
