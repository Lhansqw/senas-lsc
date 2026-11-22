# Señas LSC para iPhone

La app de iPhone reconoce las señas con la cámara frontal y propone la frase. Usa el mismo modelo y
la misma lógica que Android.

## Qué se comparte con Android

```
Kotlin común (shared/ → SenasShared.xcframework)          Swift (ios/)
────────────────────────────────────────────────          ─────────────────────────────────
Subconjuntos y asignación de manos (ConstructorFotograma)  Cámara (AVFoundation)
Características, post-procesado, filtro temporal           MediaPipe Hand + Pose Landmarker
Secuencia de señas y frase por reglas                       TensorFlow Lite (InferenciaTFLite)
Coordinador y estado (ProcesadorCompartido)                 Pantallas SwiftUI
```

Android usa Holistic Landmarker, que no existe para iOS. Como el modelo actual no usa el rostro
(`usa_rostro: false`), iOS combina Hand Landmarker y Pose Landmarker. `ConstructorFotograma` asigna
cada mano a la muñeca más cercana de la pose, como hace Holistic.

## Compilar (en una Mac)

Requisitos: macOS con Xcode 16 o posterior, JDK 17 o posterior, `brew install xcodegen cocoapods`.

```sh
sh ios/preparar.sh          # XCFramework de Kotlin, modelos de MediaPipe, proyecto y pods
open ios/SenasLSC.xcworkspace
```

En Xcode:

1. En **Signing & Capabilities** elige tu equipo en el target `SenasLSC`. También puedes poner tu
   Team ID en `DEVELOPMENT_TEAM`, en `project.yml`.
2. Ejecuta el esquema `SenasLSC` en un **iPhone real**: el simulador no tiene cámara.

El modelo de señas (`modelo.json` y `reconocedor_lsc.tflite`) se toma de
`recognizer/litert/src/main/assets/`, el mismo de Android. Si falta, la app avisa de que no hay modelo.

Sin Mac, el workflow `.github/workflows/ios.yml` compila la app en GitHub Actions (sin firmar).

## Antes de publicar: prueba de paridad

Hay que comprobar que Hand + Pose dan los mismos rasgos que Holistic, antes de dar por buena la
exactitud en iPhone:

1. Graba varias muestras en Android (modo grabación) con señas conocidas.
2. Hazlas también delante del iPhone y compara las señas reconocidas.
3. Si la mano izquierda y la derecha salen intercambiadas, revisa la asignación en
   `shared/.../ConstructorFotograma.kt` y la etiqueta de `handedness` en `ExtractorLandmarksIOS.swift`.

## Pendiente para versiones posteriores

- Historial cifrado (Keychain), modelo de lenguaje local y Gemini.
- Modo grabación de muestras.
