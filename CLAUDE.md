# CLAUDE.md — Señas LSC

Instrucciones para trabajar en este repositorio con Claude Code. Léelas completas antes de
cambiar nada.

## Qué es

App Android (Kotlin, Jetpack Compose) que reconoce señas de la **Lengua de Señas Colombiana
(LSC)** con la cámara y las convierte en texto en español.

Cadena: CameraX → MediaPipe Holistic (puntos) → reconocedor propio (LiteRT) → filtro temporal →
secuencia → propuesta de frase (reglas o Gemma con LiteRT-LM) → revisión del usuario.

Autor: Nicolás Ramos López (GitHub `Lhansqw`). Toda la documentación, los comentarios, los textos
de la app y los mensajes de commit van **en español**.

## Reglas de commits (obligatorias)

- El autor y el committer de todos los commits deben ser **`Lhansqw <lhanmls37437@gmail.com>`**.
  Antes del primer commit, comprueba con `git config user.name` y `git config user.email`.
- **Nunca** añadas `Co-Authored-By`, `Claude-Session`, «Generated with Claude Code» ni ninguna
  mención a Claude o a Anthropic en mensajes de commit, PR o archivos.
  - `.claude/settings.json` desactiva la atribución automática.
  - El hook `.githooks/commit-msg` la elimina por si acaso. Actívalo una vez con
    `git config core.hooksPath .githooks`.
- Haz **commits pequeños y reales**: un cambio coherente por commit (una clase, sus pruebas, una
  pantalla, un documento). Nada de commits vacíos o de relleno.
- Formato del mensaje: `tipo(ámbito): descripción en minúscula`. Tipos: `feat`, `fix`, `test`,
  `docs`, `build`, `refactor`. Ejemplos de ámbito: `core`, `sequence`, `recognizer`, `app`,
  `training`.
- **Antes de cada commit con código, `gradlew.bat test` debe terminar en `BUILD SUCCESSFUL`.**
  No hagas commit con pruebas en rojo.
- Tras una tanda de commits, comprueba `git log --format="%h %an <%ae> | %cn <%ce> %s" -20` y
  sube con `git push origin main`.

## Comandos

```bat
gradlew.bat test                 :: todas las pruebas unitarias (JVM)
gradlew.bat assembleDebug        :: APK de depuración (app\build\outputs\apk\debug\)
gradlew.bat installDebug         :: instala en el teléfono conectado por USB
gradlew.bat :sequence:test       :: pruebas de un módulo
```

La primera compilación descarga el modelo de MediaPipe (tarea `descargarModeloHolistic`).

## Estructura

| Módulo | Tipo | Contenido |
|---|---|---|
| `:app` | Android | Pantallas Compose, ViewModels, cámara, IA, historial, vocabulario |
| `:core:model` | Kotlin puro | Tipos de dominio, lector y escritor JSON, normalización de texto |
| `:core:descargas` | Kotlin puro | Descarga reanudable, progreso, espacio |
| `:recognizer:api` | Kotlin puro | `Caracteristicas`, `ContratoModelo`, `LectorContrato`, `PostProcesador` |
| `:sequence` | Kotlin puro | `VentanaTemporal`, `Remuestreo`, `FiltroTemporal`, `ConstructorSecuencia`, `MotorSenas` |
| `:interpreter:api` | Kotlin puro | `PlantillaPrompt`, `ValidadorRespuesta`, `InterpreteGenerativo` |
| `:interpreter:rules` | Kotlin puro | `InterpreteReglas` |
| `:data:history` | Kotlin puro | Historial cifrado (AES-GCM) |
| `:camera` | Android | `ControladorCamara` (CameraX) |
| `:vision:landmarks` | Android | `ExtractorLandmarks` (MediaPipe Holistic), `Subconjuntos` |
| `:recognizer:litert` | Android | `ReconocedorLiteRT`, `CargadorReconocedor` |
| `:interpreter:litertlm` | Android | `MotorLiteRtLm`, `GestorModeloLenguaje` |
| `docs/` | | Documentación por fase (`fase-0X-*.md`) y diseño (`01`–`06`) |

La lógica que decide qué ve el usuario está en módulos Kotlin puro y se prueba con JUnit. Mantén
esa separación: nada de clases de Android en esos módulos.

Convenciones de código: los identificadores van sin «ñ» (`SenaConfirmada`, `ReconocedorSenas`);
los textos visibles sí la llevan y van en `strings.xml`. Las versiones están en
`gradle/libs.versions.toml` (AGP 9.4.1 con Kotlin integrado, Kotlin 2.4.20, Compose BOM
2026.08.00, CameraX 1.6.2, MediaPipe 1.1.1, LiteRT 2.3.0, LiteRT-LM 0.18.1). No las cambies sin
motivo y, si lo haces, en un commit `build:` propio.

## Contrato del modelo de señas (no romper)

Lo usa la app y lo debe respetar el entrenamiento (fase 6). Detalle completo en
`docs/fase-07-reconocedor-litert.md`.

- **Puntos:** `vision/landmarks/.../Subconjuntos.kt` (versión 1): manos 2×21, cuerpo 7 (nariz,
  hombros, codos, muñecas), rostro 39.
- **Regla del espejo:** MediaPipe recibe la imagen **sin espejo**: «mano izquierda» es la mano
  izquierda real de la persona. El entrenamiento debe extraer los puntos igual.
- **Características:** `recognizer/api/.../Caracteristicas.kt` (versión 1), **352 por
  fotograma**. Python debe calcular exactamente los mismos valores en el mismo orden.
- **Ventana:** 1 000 ms remuestreados a `fotogramas` instantes equiespaciados, por vecino más
  cercano y, en caso de empate, el anterior (`sequence/.../Remuestreo.kt`).
- **Modelo:** entrada `float32[1, fotogramas, 352]`, salida `float32[1, etiquetas]` con
  **logits**, sin softmax.
- **Archivos:** `recognizer/litert/src/main/assets/reconocedor_lsc.tflite` y `modelo.json`
  (formato en `docs/modelo.ejemplo.json`; lo valida `LectorContrato`).
- Si cambias algo del contrato, sube la versión correspondiente en Kotlin **y** en Python, y
  añade pruebas.

## Honestidad y privacidad (obligatorio)

- No inventes métricas ni resultados. Cualquier cifra que aparezca en la documentación o en la
  app debe salir de un script que la reproduzca.
- La app reconoce un **vocabulario limitado**; nunca digas que traduce toda la LSC.
- No incluyas modelos «de ejemplo» entrenados con datos falsos: darían señas inventadas.
- Las imágenes no se guardan ni se envían. La IA en la nube (fase 10) solo enviará texto y solo
  con consentimiento.
- Los datos de entrenamiento no se suben a git (`training/data/` está en `.gitignore`). Respeta
  sus licencias y cítalas en `training/DATOS.md`.

## Estado de las fases

| Fase | Estado |
|---|---|
| 1–5 (alcance, proyecto, módulos, cámara, MediaPipe) | ✅ |
| 6 Datos y entrenamiento | ⏸️ **siguiente** |
| 7 Reconocedor LiteRT (contra el contrato) | ✅ |
| 8 Modelo de lenguaje local (Gemma 4 E2B, LiteRT-LM) | ✅ |
| 9 Interfaz de resultados, historial cifrado, vocabulario | ✅ |
| 10 IA en la nube (opcional) | pendiente: elegir proveedor |
| 11 Optimización con mediciones reales | pendiente |
| 12 Versión instalable firmada | pendiente |

## Plan de la fase 6 (entrenamiento)

Crear `training/` en Python 3.12, con entorno virtual y `requirements.txt` con versiones fijadas
y comprobadas.

1. `training/DATOS.md`: origen, licencia, fecha de descarga y cita de cada conjunto. LSC70 (CC BY
   4.0, DOI 10.17632/9ssyn8tff5.2) es imágenes en transiciones de 6 fotogramas por voluntario y
   seña.
2. `training/senas/caracteristicas.py`: puerto exacto de `Caracteristicas.kt`, `Subconjuntos.kt` y
   `Remuestreo.kt`. Crear un **archivo de casos de prueba compartido** (JSON en
   `training/pruebas/casos_caracteristicas.json`) que se compruebe tanto en Python (pytest) como en
   Kotlin (prueba JUnit en `:recognizer:api`), para garantizar que ambos dan los mismos números.
3. `training/extraer.py`: puntos con MediaPipe (misma versión de los modelos que la app, imagen sin
   espejo) → archivos `.npz` por muestra con persona, seña y repetición.
4. `training/entrenar.py`: Conv1D + GRU bidireccional (Keras 3), clase `sin_sena` si hay datos de
   ella, aumento de datos sobre puntos y **partición por persona** (entrenamiento, validación y
   prueba sin personas compartidas).
5. `training/evaluar.py`: exactitud, precisión, recall y F1 por seña, macro-F1, matriz de
   confusión, calibración (escalado de temperatura) y umbral de energía, siempre con personas no
   vistas. Informe en `training/outputs/informe.md` con las cifras reales.
6. `training/exportar.py`: `.tflite` (salida en logits) y `modelo.json` con vocabulario, ventana,
   temperatura, umbral y métricas reales; validarlo cargándolo en la app.
7. Probar en el teléfono y documentar en `docs/fase-06-entrenamiento.md`.
