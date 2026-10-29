// Modelo de lenguaje en el teléfono con LiteRT-LM, y gestión del archivo del modelo
// (descarga reanudable o importación desde un archivo).
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.lhansqw.senaslsc.interpreter.litertlm"
    compileSdk = 37

    defaultConfig {
        // LiteRT-LM necesita Android 8.0 (API 26) para la inferencia en CPU.
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":interpreter:api"))
    api(project(":core:descargas"))
    implementation(libs.litertlm.android)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
