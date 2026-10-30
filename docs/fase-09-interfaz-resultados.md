# Fase 9 · Interfaz de resultados, historial y vocabulario

## Qué incluye

### Acciones sobre la frase (pantalla de cámara)

Debajo de la propuesta aparecen cuatro botones:

| Botón | Qué hace |
|---|---|
| **Editar** | Abre un diálogo para corregir la frase. La corrección es **definitiva**: la propuesta pasa a «Frase corregida por ti» y se cancela cualquier mejora de la IA que estuviera en curso. Si la respuesta de la IA llega después, no sustituye tu texto |
| **Copiar** | Copia la frase al portapapeles. Desde Android 13 el sistema muestra su propio aviso; en versiones anteriores, la app muestra uno |
| **Compartir** | Abre el selector de Android para enviar la frase por cualquier app |
| **Guardar** | Guarda la frase en el historial. Solo aparece si el historial está activado |

### Historial (Inicio → Historial)

- **Opcional y desactivado por defecto.** Se activa con un interruptor en la propia pantalla.
- **Solo texto:** la frase, las glosas de las señas, su origen (reglas, IA del teléfono, IA en la
  nube o corregida por ti) y la fecha. Nunca imágenes, vídeo ni puntos de referencia.
- **Cifrado en reposo:**
  - AES-256-GCM, con un vector de inicialización nuevo en cada escritura.
  - La clave está en el **Android Keystore**: no se puede exportar y, en teléfonos con hardware
    seguro, no sale de él.
  - El archivo está, además, en el almacenamiento privado de la app y excluido de las copias de
    seguridad.
- **Escritura atómica:** si la app se cierra a mitad de una escritura, queda la versión anterior
  completa.
- Búsqueda sin tildes por texto o por glosa, fechas relativas («Hoy, 14:32», «Ayer, 09:05»),
  copiar o eliminar cada frase y borrar todo con confirmación.
- Guarda como máximo 500 frases; al superarlas, se eliminan las más antiguas.
- Si al desinstalar se pierde la clave, el historial anterior queda ilegible. La app lo detecta,
  avisa y empieza uno nuevo en lugar de fallar.

### Vocabulario compatible (Inicio → Vocabulario compatible)

- Lista **exactamente** las señas que reconoce el modelo instalado, leídas de `modelo.json` sin
  cargar el modelo.
- Agrupadas en palabras, números (por valor) y letras, con búsqueda sin tildes y filtro por tipo.
- Muestra el nombre y la versión del modelo, sus métricas y los datos de entrenamiento con su
  licencia.
- Sin modelo (ahora) explica que todavía no se reconoce ninguna seña.

## Código nuevo

| Módulo | Archivo | Qué hace |
|---|---|---|
| `:core:model` | `json/EscritorJson.kt` | Escritor JSON compatible con el lector |
| | `texto/Normalizar.kt` | Búsqueda sin tildes ni puntuación, conservando la «ñ» |
| `:data:history` (nuevo, Kotlin puro) | `EntradaHistorial.kt`, `FormatoHistorial.kt` | Modelo y formato versionado |
| | `RepositorioHistorial.kt` | Límite, búsqueda y recuperación ante datos ilegibles |
| | `CifradorAesGcm.kt`, `AlmacenArchivo.kt` | Cifrado autenticado y escritura atómica |
| | `FormatoFecha.kt` | Fechas relativas en español |
| `:app` | `historial/ClaveKeystore.kt` | Clave AES-256 en el Android Keystore |
| | `historial/ServicioHistorial.kt` | Acceso en un único hilo de E/S |
| | `historial/PantallaHistorial.kt` | Pantalla de historial |
| | `vocabulario/FiltroVocabulario.kt` | Filtro y agrupación (Kotlin puro) |
| | `vocabulario/PantallaVocabulario.kt` | Pantalla de vocabulario |
| | `reconocimiento/DialogoEdicion.kt` | Corrección de la frase |
| | `ui/AccionesTexto.kt` | Copiar y compartir |

## Pruebas nuevas (230 en total)

- `EscritorJsonTest`: escapes, orden de campos y lectura de vuelta sin pérdida.
- `NormalizarTest`: tildes, eñe y puntuación.
- `FormatoHistorialTest`, `RepositorioHistorialTest`: cifrado en reposo con un cifrador de
  prueba, límite, búsqueda y clave perdida.
- `CifradoYArchivoTest`: **AES-GCM real** de Java. Comprueba que el IV cambia en cada escritura,
  que se detecta una manipulación, que con otra clave no se descifra, que se rechazan bloques mal
  formados, que el archivo se escribe de forma atómica y que el repositorio, con AES y archivo
  reales, sobrevive a una nueva instancia.
- `FormatoFechaTest`, `FiltroVocabularioTest`, `ConversionTest`.
- `CoordinadorReconocimientoTest`: la corrección del usuario prevalece sobre la IA.

## Cómo comprobar que funciona

```bash
git pull
gradlew.bat test
```

Resultado esperado: `BUILD SUCCESSFUL` y 230 pruebas.

En el teléfono:

1. **Inicio** muestra los botones «Vocabulario compatible» e «Historial».
2. **Vocabulario:** sin modelo, debe explicar que aún no se reconoce ninguna seña.
3. **Historial:** está desactivado. Actívalo y vuelve; la preferencia se recuerda al cerrar la app.
4. Los botones Editar, Copiar, Compartir y Guardar aparecen bajo cada propuesta, así que se podrán
   probar en la cámara cuando haya modelo de señas (fase 6).
5. **TalkBack:** los títulos de las pantallas y de los grupos se anuncian como encabezados, y los
   botones tienen su nombre.

## Qué se verificó antes de subir

- Toda la lógica pura, incluidos el cifrado AES-GCM y el archivo atómico, se compiló con
  Kotlin 2.4.20 y se probó: 230 pruebas correctas.
- El código Android (Keystore, Compose, portapapeles) se revisó a mano y con el compilador.
  **No se pudo compilar entero** en el entorno donde se preparó el commit.
