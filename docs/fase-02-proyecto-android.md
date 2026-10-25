# Fase 2 · Proyecto Android

## Qué incluye

| Archivo | Para qué |
|---|---|
| `settings.gradle.kts` | Repositorios (Google, Maven Central) y módulos del proyecto |
| `build.gradle.kts` | Plugins del proyecto, declarados sin aplicar |
| `gradle/libs.versions.toml` | Catálogo con **todas** las versiones en un solo sitio |
| `gradle.properties` | Memoria de Gradle, caché y configuración de AndroidX |
| `gradle/wrapper/*`, `gradlew`, `gradlew.bat` | Gradle 9.6.1, descargado automáticamente la primera vez |
| `app/build.gradle.kts` | Módulo de la app: SDK, Compose, dependencias |
| `app/src/main/AndroidManifest.xml` | Sin permisos todavía; copias de seguridad desactivadas por privacidad |
| `app/src/main/kotlin/.../MainActivity.kt` | Punto de entrada; pantalla de borde a borde |
| `.../ui/PantallaInicio.kt` | Primera pantalla: estado del dispositivo y limitaciones |
| `.../ui/theme/Tema.kt` | Tema Material 3 claro y oscuro, con color dinámico en Android 12+ |
| `.../dispositivo/CapacidadDispositivo.kt` | Decide qué IA puede ejecutarse en local según API, RAM y núcleos |
| `.../dispositivo/LectorPerfilDispositivo.kt` | Lee esos datos del sistema |
| `app/src/test/.../EvaluadorCapacidadTest.kt` | 7 pruebas JUnit de la lógica anterior |

## Versiones

| Componente | Versión | Motivo |
|---|---|---|
| Android Gradle Plugin | 9.4.0 | Estable de septiembre de 2026; requiere Gradle 9.6+ y JDK 17+ |
| Gradle | 9.6.1 | Mínimo de AGP 9.4 |
| Kotlin | 2.4.20 | Estable de septiembre de 2026 |
| Compose BOM | 2026.08.00 | Compose 1.12; requiere `compileSdk` 37 y AGP 9.1.1+ |
| `compileSdk` / `targetSdk` | 37 | Máximo de AGP 9.4 y requisito de Compose 1.12 |
| `minSdk` | 26 | Mínimo de LiteRT-LM (doc. 04) |

AGP 9 trae **Kotlin integrado**: el plugin `org.jetbrains.kotlin.android` ya no se aplica. El
archivo raíz fija KGP 2.4.20 en el *classpath* para que coincida con el plugin del compilador de
Compose, como indica la guía de migración de AGP 9.

## Cómo comprobar que funciona

1. **Abrir el proyecto.** Android Studio → *File → Open* → carpeta `senas-lsc`. La primera
   sincronización descarga Gradle 9.6.1 y las dependencias (varios minutos).
   - Si pide actualizar AGP o alguna librería, **no lo aceptes todavía**: avísame y lo actualizamos
     en un commit propio.
2. **JDK.** *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK*:
   «jbr-21» (el que trae Android Studio) o cualquier JDK 17 o superior.
3. **SDK 37.** Si falta, Android Studio ofrece instalarlo; acepta.
4. **Pruebas unitarias**, desde la terminal de Android Studio:
   ```bash
   ./gradlew test
   ```
   En Windows (cmd o PowerShell): `gradlew.bat test`. Resultado esperado: `BUILD SUCCESSFUL`,
   7 pruebas superadas. Informe: `app/build/reports/tests/testDebugUnitTest/index.html`.
5. **Ejecutar en el teléfono**: conecta el teléfono con depuración USB y pulsa ▶ *Run 'app'*.
   Debe abrirse «Señas LSC» con la tarjeta *Tu dispositivo* mostrando tu versión de Android,
   tu RAM y si el modelo de lenguaje local estaría disponible.
6. **Vista previa** (opcional): abre `PantallaInicio.kt` y usa el panel *Split* para ver
   «Gama media» sin teléfono.

## Qué se verificó antes de subir

- La lógica de `CapacidadDispositivo.kt` y sus 7 pruebas se compilaron y ejecutaron con
  Kotlin 2.4.20: todas correctas.
- La compilación completa de Android (Gradle, AGP, Compose) **no se pudo ejecutar** en el entorno
  donde se preparó el commit, porque no tiene acceso a los repositorios de Google y Maven. Se
  comprueba con los pasos de arriba.
