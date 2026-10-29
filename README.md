# Señas LSC

Aplicación Android que reconoce señas de la **Lengua de Señas Colombiana (LSC)** con la cámara
del teléfono y las convierte en texto en español mediante inteligencia artificial.

> **Estado:** fase 3 — estructura modular: contratos, filtro temporal, validador del modelo de
> lenguaje e intérprete por reglas, con pruebas. Todavía no usa la cámara ni reconoce señas. El reconocedor funcionará con un **vocabulario limitado y verificado**; no
> traduce toda la LSC.

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

## Compilar y probar

```bash
./gradlew test             # pruebas unitarias (Windows: gradlew.bat test)
./gradlew assembleDebug    # APK de depuración en app/build/outputs/apk/debug/
```

Requisitos: Android Studio reciente, JDK 17 o superior y SDK de Android 37 (ver
[docs/05-entorno.md](docs/05-entorno.md)).

## Estructura del repositorio

```
senas-lsc/
├── app/                   Aplicación Android (Kotlin, Jetpack Compose)
├── core/model/            Tipos de dominio, sin dependencias de Android
├── recognizer/api/        Contrato del reconocedor visual
├── sequence/              Ventanas, filtro temporal y secuencia de señas
├── interpreter/api/       Contrato del intérprete, instrucciones y validador
├── interpreter/rules/     Intérprete por reglas (sin modelo de lenguaje)
└── docs/                  Documentación del proyecto
```

Se añadirán en sus fases: `camera/` (4), `vision/` (5), `training/` (6, Python),
`recognizer/litert/` (7), `interpreter/litertlm/` (8), `data/history/` (9).

## Autor

Nicolás Ramos López ([@Lhansqw](https://github.com/Lhansqw))
