// Lógica de descarga de archivos grandes (reanudación, progreso, espacio). Kotlin Multiplatform:
// las reglas y los formatos están en commonMain; la descarga y la copia (java.io, java.net) solo
// existen en el target jvm, que es el que usa Android.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}
