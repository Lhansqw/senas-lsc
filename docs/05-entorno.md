# 05 · Entorno de desarrollo

## Programas que hay que instalar

| Programa | Para qué | Notas |
|---|---|---|
| **Android Studio** (última versión estable) | Editor, compilación, emulador, depuración | Incluye el JDK (JetBrains Runtime): no hace falta instalar Java aparte |
| **Android SDK** | Compilar la app | Desde *SDK Manager*: la plataforma más reciente que proponga Android Studio, *Build-Tools* y *Platform-Tools* (`adb`) |
| **Git** | Control de versiones | Configurar la identidad del autor (ver abajo) |
| **Python 3.12** | Preparar datos, entrenar y exportar modelos | Se confirma la versión exacta compatible con TensorFlow en la fase 6 |
| **Teléfono Android físico** | Pruebas reales con la cámara | Android 8.0 (API 26) o superior. Activar *Opciones de desarrollador* → *Depuración USB*. El emulador no sirve para probar la cámara frontal con señas |
| Controlador USB del fabricante (solo Windows) | Que `adb` vea el teléfono | Samsung, Xiaomi, Motorola, etc. publican el suyo. En macOS no hace falta |
| Google Colab o GPU NVIDIA (opcional) | Entrenar más rápido | Los modelos sobre puntos de referencia se entrenan bien en CPU |

## Configuración de Git

Todos los commits del proyecto salen solo a nombre del autor:

```bash
git config user.name "Lhansqw"
git config user.email "lhanmls37437@gmail.com"
```

Comprobación del historial (no debe aparecer otro nombre ni otro correo):

```bash
git log --format="%h  %an <%ae>  |  %cn <%ce>  %s"
```

## Comprobación del entorno

Después de instalar:

```bash
git --version
python --version          # 3.12.x
adb version               # tras añadir platform-tools al PATH
adb devices               # el teléfono debe aparecer como "device"
```

### En una Mac

Todo lo de arriba funciona igual en macOS. La app de Android compila en cualquier Mac; el
entrenamiento necesita Apple Silicon (M1 o posterior), porque TensorFlow ya no publica versiones
para Mac con procesador Intel. Python se instala con
[Homebrew](https://brew.sh): `brew install python@3.12`. El SDK de Android queda en
`~/Library/Android/sdk`; para usar `adb` desde la terminal:

```bash
echo 'export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"' >> ~/.zshrc
```

Los comandos de Gradle se escriben `./gradlew ...` y los scripts son los `.sh` de `scripts/` y
`training/`. La app de iPhone y de Apple Watch solo se compila en una Mac: ver
[ios/README.md](../ios/README.md).

En Android Studio: *File → New → New Project → Empty Activity* y ejecutar en el teléfono. Si
arranca, el entorno está listo para la fase 2.
