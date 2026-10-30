# Fase 12 · Versión instalable

> **Estado:** todo preparado. Falta generar el APK firmado en tu PC y pasar la lista de
> comprobación en un teléfono distinto al de desarrollo.

## Qué hay

| Pieza | Para qué |
|---|---|
| `app/proguard-rules.pro` | R8 conserva MediaPipe, LiteRT y LiteRT-LM (su código nativo los busca por nombre; sin estas reglas la versión de producción se cierra al cargar los modelos) |
| `app/build.gradle.kts` | Firma desde `keystore.properties`; versión desde `gradle.properties`; `-Psenas.r8=false` para compilar sin R8 |
| `scripts/crear_firma.ps1` | Crea la clave de firma fuera del repositorio (una sola vez) |
| `scripts/version_instalable.ps1` | Pruebas → APK y AAB firmados → verifica la firma → `dist\` con SHA-256 |
| Pantalla «Acerca de» | Qué hace y qué no, privacidad, modelo instalado, cita de LSC70 (obligatoria por CC BY 4.0) y licencias |
| [`docs/ficha-modelo.md`](ficha-modelo.md) | Ficha del modelo con las cifras medidas; la regenera `training/ficha.py` |

## Pasos

```powershell
cd C:\dev\senas-lsc
git pull origin main

# 1. Solo la primera vez: crear la clave de firma (pide una contraseña).
powershell -ExecutionPolicy Bypass -File .\scripts\crear_firma.ps1

# 2. Cada versión: subir senas.versionCode en gradle.properties y generar.
powershell -ExecutionPolicy Bypass -File .\scripts\version_instalable.ps1
```

El resultado queda en `dist\senas-lsc-1.0.0.apk` (instalar directamente) y
`dist\senas-lsc-1.0.0.aab` (Google Play), con `dist\SHA256SUMS.txt`.

**Guarda una copia de la clave** (`%USERPROFILE%\senas-lsc-firma\senas-lsc.jks`) y de su
contraseña. Sin ellas no se puede instalar una actualización encima de esta versión.

Si la app de producción se cierra al abrir la cámara y la de depuración no, repite con
`-SinR8`: si así funciona, falta alguna regla en `proguard-rules.pro` (el error exacto aparece
con `adb logcat`).

## Lista de comprobación en otro teléfono

Instalación limpia en un teléfono que nunca haya tenido la app (`adb install dist\senas-lsc-1.0.0.apk`
o copiando el APK y abriéndolo).

| # | Comprobación | Resultado esperado |
|---|---|---|
| 1 | Abrir la app | Inicio con la versión 1.0.0 y la tarjeta del dispositivo |
| 2 | «Abrir cámara» y rechazar el permiso | Explicación y opción de volver a pedirlo; sin cierre |
| 3 | Aceptar el permiso | Vista previa, puntos de manos y cuerpo, FPS y latencias |
| 4 | Señar HOLA, YO, NOMBRE | Aparecen como señas confirmadas |
| 5 | Dejar las manos quietas fuera de cámara 3 s | Aparece «Reposo» y bajan los FPS |
| 6 | «Interpretar» | Propuesta de frase editable; copiar y compartir funcionan |
| 7 | Modo avión y repetir 3–6 | Funciona igual (todo es local) |
| 8 | Girar el teléfono | No se pierde la cámara ni la frase |
| 9 | «Vocabulario compatible» | 42 señas, buscador y filtros |
| 10 | «Acerca de» | Modelo 0.1.0, cita de LSC70 y licencias |
| 11 | Historial: activarlo, guardar una frase, cerrar y abrir la app | La frase sigue; «Borrar todo el historial» la elimina |
| 12 | IA local (si el teléfono es compatible) | Descarga solo al pedirla; propuestas marcadas como IA |
| 13 | TalkBack activado en inicio y cámara | Todos los botones se anuncian con su nombre |
| 14 | Desinstalar | No quedan datos (la app no hace copias de seguridad) |

Anota el modelo del teléfono, la versión de Android y los p50/p95 de la cámara: sirven también
para la fase 11.
