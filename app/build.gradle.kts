plugins {
    alias(libs.plugins.android.application)
    // AGP 9 compila Kotlin de forma integrada: no se aplica org.jetbrains.kotlin.android.
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.lhansqw.senaslsc"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lhansqw.senaslsc"
        // API 26 (Android 8.0): mínimo de LiteRT-LM para el modelo de lenguaje local.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // Los modelos se leen tal cual desde los assets: no se comprimen en el APK.
        noCompress += listOf("task", "tflite")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":recognizer:api"))
    implementation(project(":sequence"))
    implementation(project(":interpreter:api"))
    implementation(project(":interpreter:rules"))
    implementation(project(":data:history"))
    implementation(project(":camera"))
    implementation(project(":vision:landmarks"))
    implementation(project(":recognizer:litert"))
    implementation(project(":interpreter:litertlm"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.camera.compose)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
