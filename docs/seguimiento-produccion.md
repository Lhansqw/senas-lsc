# Seguimiento: camino a producción (1.0.0)

Diario de trabajo para retomar donde se dejó. Lo más reciente, arriba.

## Estado al cerrar el 15/11/2026

**Rama actual:** `rendimiento/mediapipe-manos-cuerpo`, **con cambios sin commit** (falta escribir la
contraseña de GPG para firmar). Hay que hacer dos commits:

1. `fix(build): conservar Flogger con R8 para que MediaPipe arranque`. Solo `app/proguard-rules.pro`.
2. `perf(vision): MediaPipe Hand + Pose en vez de Holistic`. Incluye `ExtractorLandmarks.kt`,
   `vision/landmarks/build.gradle.kts`, `.gitignore`, `AnalizadorLandmarks.kt`,
   `training/extraer.py`, `training/paridad_manos_pose.py` y este archivo.

**Esperando:** medir en el teléfono el APK con Hand + Pose. Ya está copiado en Descargas del
teléfono (SHA-256 `ed2af8bf…a05a`), pero falta instalarlo y medir.

## Siguientes pasos

1. Instalar el APK nuevo desde Descargas, abrir la cámara, tocar «Análisis: … FPS» (diagnóstico),
   señar 20 s y anotar los FPS, la latencia de Análisis p50/p95 y los ms de MediaPipe por fotograma.
   - **Antes, con Holistic:** 4,1 FPS · Análisis p50 214 / p95 281 ms · MediaPipe 181 ms por fotograma
     · modelo de señas p50 5,3 / p95 23,6 ms.
   - Si mejora y reconoce mejor → hacer los commits, abrir el PR y fusionarlo.
   - Si no compensa → volver a Holistic (`git checkout` de esos archivos) y buscar velocidad por
     otro lado (menos resolución de análisis, preparación de la imagen: ~33 ms por fotograma).
2. Si sigue lento al confirmar señas: ajustar `ConfiguracionFiltro` (`sequence/.../FiltroTemporal.kt`).
   Ahora pide 3 ventanas seguidas con confianza ≥ 0,70.
3. Más adelante: reentrenar con puntos de Hand + Pose para recuperar los 2,8 puntos de exactitud
   (`paridad_manos_pose.py` ya sabe extraerlos de LSC70; LSC50 solo trae puntos de Holistic).
4. Pasar la lista de 14 comprobaciones de `docs/fase-12-version-instalable.md` (debe mostrar 86 señas).
5. Copia de seguridad de la clave de firma (`%USERPROFILE%\senas-lsc-firma\senas-lsc.jks`) y de su
   contraseña fuera del PC.
6. Si se publica en Google Play: política de privacidad, formulario de seguridad de datos, ficha y
   prueba cerrada (12 testers, 14 días).

## Hecho

- **PR #5 fusionado:** cita de LSC50 en «Acerca de» (Android e iOS), 5 señas ocultas (CÓMO ESTÁS,
  MUJER, NIÑA, QUÉ y SENTIR, con F1 0 %) mediante `salida.ocultas` en `modelo.json`, CI de Android y
  arreglo de la descarga del modelo de MediaPipe (el `.task` es un ZIP con relleno al principio).
- **Clave de firma creada.** Huella SHA-256 del certificado:
  `AC:7D:6C:75:63:14:62:D9:8A:1B:86:6C:29:FA:AE:D7:0D:50:05:49:07:78:6F:24:FB:6E:49:8A:81:A1:00:7E`.
- **R8 rompía la cámara:** MediaPipe usa Flogger, que busca sus clases por nombre en la pila
  (`no caller found on the stack for: o51`). Se arregló con `-keep class com.google.common.flogger.**`.
  Comprobado en el teléfono; falta el commit (punto 1 de arriba).
- **Paridad Holistic frente a Hand + Pose** (469 muestras de prueba de LSC70, modelo 0.3.0):
  Holistic 90,0 % · Hand + Pose 87,2 % · eligen la misma seña en el 90,8 %. Se decidió seguir con
  Hand + Pose por velocidad.

## Notas prácticas

- **Teléfono de prueba:** Xiaomi Redmi 9 (M2004J19C), Android 11, arm64, por depuración inalámbrica.
  - MIUI bloquea `adb install` (`INSTALL_FAILED_USER_RESTRICTED`) y `adb shell input`. Para instalar,
    copiar el APK con `adb push` a `/sdcard/Download/` e instalarlo desde Archivos.
  - En Git Bash, usar `MSYS_NO_PATHCONV=1` con `adb`, o convierte `/sdcard/...` en una ruta de Windows.
  - Comprobar que se instaló el APK correcto: `adb shell pm path com.lhansqw.senaslsc` y comparar
    el `sha256sum` con `dist\SHA256SUMS.txt`. Una vez MIUI dejó la versión antigua sin avisar.
- **Generar la versión firmada:** `powershell -ExecutionPolicy Bypass -File .\scripts\version_instalable.ps1`
  (lee la contraseña de `keystore.properties`, que git ignora).
- **Commits firmados con GPG:** la caché de la contraseña caduca. Comprobar con
  `"C:\Program Files\GnuPG\bin\gpg-connect-agent.exe" "keyinfo --list" /bye` (un `1` antes de `P`
  = en caché) y, si no, `"prueba" | & "C:\Program Files\GnuPG\bin\gpg.exe" --clearsign` en PowerShell.
  Usar el gpg de Gpg4win, no el de Git Bash.
