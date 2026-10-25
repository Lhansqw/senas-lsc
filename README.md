# Señas LSC

Aplicación Android que reconoce señas de la **Lengua de Señas Colombiana (LSC)** con la cámara
del teléfono y las convierte en texto en español mediante inteligencia artificial.

> **Estado:** fase 1 — alcance, viabilidad y arquitectura. Todavía no hay código de la
> aplicación. El reconocedor funcionará con un **vocabulario limitado y verificado**; no traduce
> toda la LSC.

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

## Estructura prevista del repositorio

```
senas-lsc/
├── app/                 Aplicación Android (Kotlin, Jetpack Compose)
├── core/                Tipos de dominio y utilidades sin dependencias de Android
├── camera/              Captura con CameraX
├── vision/              Extracción de puntos de referencia con MediaPipe
├── recognizer/          Contrato del reconocedor + implementación LiteRT
├── sequence/            Filtro temporal y construcción de la secuencia
├── interpreter/         Contrato del intérprete + implementaciones (reglas, local, remota)
├── data/                Historial local
├── training/            Python: preparación de datos, entrenamiento, evaluación y exportación
└── docs/                Documentación del proyecto
```

## Autor

Nicolás Ramos López ([@Lhansqw](https://github.com/Lhansqw))
