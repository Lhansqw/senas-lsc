// Intérprete determinista: siempre disponible, sin modelo de lenguaje y sin Internet.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    api(project(":interpreter:api"))
}
