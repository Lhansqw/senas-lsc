# Señas LSC

Aplicación Android que reconoce señas de la **Lengua de Señas Colombiana (LSC)** con la cámara
del teléfono y las convierte en texto en español mediante inteligencia artificial.

> **Estado:** fase 9 — interfaz de resultados completa: editar, copiar, compartir, historial
> opcional cifrado y vocabulario compatible; reconocimiento con LiteRT listo para el modelo de la
> fase 6 y modelo de lenguaje opcional en el teléfono. El reconocedor funcionará con un
> **vocabulario limitado y verificado**; no traduce toda la LSC.

## Cómo funciona (resumen)

```
Cámara frontal → puntos de referencia (MediaPipe) → reconocedor de señas (modelo propio, LiteRT)
   → filtro temporal y rechazo de señas desconocidas → secuencia de señas confirmadas
   → propuesta de frase (modelo de lenguaje, opcional) → revisión y corrección del usuario
```

Dos componentes de IA, independientes entre sí:

1. **Reconocedor visual**: modelo entrenado con datos de LSC que clasifica secuencias de puntos
   de referencia de manos, cuerpo y rostro. Devuelve etiquetas y confianza, o «desconocida».
2. **Intérprete de texto**: modelo de lenguaje que ordena las señas confirmadas en una frase
   propuesta. Nunca añade señas que el reconocedor no detectó y su salida siempre se muestra
   separada, como propuesta revisable.

## Documentación

| Documento | Contenido |
|---|---|
| [01 · Viabilidad](docs/01-viabilidad.md) | Qué es viable, qué no y riesgos |
| [02 · Arquitectura](docs/02-arquitectura.md) | Etapas, módulos de Gradle y contratos entre componentes |
| [03 · Datos de LSC](docs/03-datos-lsc.md) | Conjuntos de datos disponibles, licencias y protocolo de grabación |
| [04 · Modelos de IA](docs/04-modelos.md) | Modelo visual, modelo de lenguaje, local frente a nube |
| [05 · Entorno](docs/05-entorno.md) | Programas que hay que instalar |
| [06 · MVP y fases](docs/06-mvp-y-fases.md) | Alcance del MVP, vocabulario inicial y plan de fases |
| [Fase 2 · Proyecto Android](docs/fase-02-proyecto-android.md) | Versiones, archivos y cómo comprobar que compila |
| [Fase 3 · Estructura modular](docs/fase-03-estructura-modular.md) | Módulos, lógica de confirmación de señas, validador y pruebas |
| [Fase 4 · Cámara](docs/fase-04-camara.md) | CameraX, permiso, descarte de fotogramas y cómo probarlo en el teléfono |
| [Fase 5 · MediaPipe](docs/fase-05-mediapipe.md) | Holistic Landmarker, puntos que se guardan, regla del espejo y cómo probarlo |
| [Fase 6 · Entrenamiento](docs/fase-06-entrenamiento.md) | Extracción de puntos, entrenamiento, evaluación por persona y exportación a LiteRT |
| [Fase 7 · Reconocedor LiteRT](docs/fase-07-reconocedor-litert.md) | Contrato del modelo, características, `modelo.json` y flujo de reconocimiento |
| [Fase 8 · Modelo de lenguaje](docs/fase-08-modelo-lenguaje.md) | IA local con LiteRT-LM, instalación del modelo, validación y respaldo |
| [Fase 9 · Interfaz de resultados](docs/fase-09-interfaz-resultados.md) | Edición, historial cifrado, vocabulario compatible |

## Compilar y probar

```bash
./gradlew test             # pruebas unitarias (Windows: gradlew.bat test)
./gradlew assembleDebug    # APK de depuración en app/build/outputs/apk/debug/
```

Requisitos: Android Studio reciente, JDK 17 o superior y SDK de Android 37 (ver
[docs/05-entorno.md](docs/05-entorno.md)). La primera compilación necesita Internet para descargar
el modelo de MediaPipe.

## Estructura del repositorio

```
senas-lsc/
├── app/                   Aplicación Android (Kotlin, Jetpack Compose)
├── core/model/            Tipos de dominio y lector JSON, sin dependencias de Android
├── core/descargas/        Descarga reanudable, progreso y espacio (Kotlin puro)
├── data/history/          Historial cifrado (AES-GCM) y su formato (Kotlin puro)
├── recognizer/api/        Contrato del reconocedor visual
├── sequence/              Ventanas, filtro temporal y secuencia de señas
├── interpreter/api/       Contrato del intérprete, instrucciones y validador
├── interpreter/rules/     Intérprete por reglas (sin modelo de lenguaje)
├── camera/                Captura con CameraX (librería Android)
├── vision/landmarks/      Puntos de referencia con MediaPipe (librería Android)
├── recognizer/litert/     Reconocedor con LiteRT (librería Android; ver MODELO.md)
├── interpreter/litertlm/  Modelo de lenguaje con LiteRT-LM y gestión de su archivo
└── docs/                  Documentación del proyecto
```

Se añadirán en sus fases: `training/` (6, Python) e `interpreter/remote/` (10).

## Autor

Nicolás Ramos López ([@Lhansqw](https://github.com/Lhansqw))
