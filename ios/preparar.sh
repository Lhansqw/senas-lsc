#!/bin/sh
# Prepara el proyecto de iPhone y Apple Watch en una Mac.
# Requisitos: Xcode 16+, JDK 17+, Homebrew (para xcodegen) y CocoaPods.
#   brew install xcodegen cocoapods
set -eu
cd "$(dirname "$0")"

# 1. Lógica compartida en Kotlin (la misma que usa Android).
(cd .. && ./gradlew :shared:assembleSenasSharedReleaseXCFramework --console=plain)

# 2. Modelos de MediaPipe para iOS. Solo se descargan si faltan.
mkdir -p Modelos
descargar() {
  if [ ! -s "Modelos/$1" ]; then
    echo "Descargando $1"
    curl -fL --retry 3 -o "Modelos/$1.descargando" "$2"
    mv "Modelos/$1.descargando" "Modelos/$1"
  fi
}
descargar hand_landmarker.task \
  https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task
descargar pose_landmarker_full.task \
  https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/1/pose_landmarker_full.task

# 3. Modelo de señas: lo produce el entrenamiento (training/) y vive con los assets de Android.
if [ ! -f ../recognizer/litert/src/main/assets/reconocedor_lsc.tflite ]; then
  echo "Aviso: falta recognizer/litert/src/main/assets/reconocedor_lsc.tflite; la app dirá que no hay modelo." >&2
fi

# 4. Proyecto Xcode y dependencias nativas.
xcodegen generate
pod install

echo "Listo. Abre ios/SenasLSC.xcworkspace en Xcode."
