// Implementación del reconocedor de señas con LiteRT (CompiledModel).
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.lhansqw.senaslsc.recognizer.litert"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":recognizer:api"))
    implementation(libs.litert)

    testImplementation(libs.junit)
}
