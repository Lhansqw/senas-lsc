// Tipos de dominio compartidos. Kotlin Multiplatform sin dependencias de Android: lo usan la
// app Android (target jvm) y la de iPhone (targets iOS).
// Los targets, JVM 17 y JUnit se configuran en el build.gradle.kts raíz.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}
