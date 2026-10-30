# Fase 10 · IA del teléfono frente a IA en internet

> **Estado:** hecha. Falta ejecutar la comparativa con tu clave y probarla en el teléfono.

## Qué se eligió

**Gemini** (Google) mediante `models.generateContent`, que la documentación oficial describe como
«fully supported» (octubre de 2026). Modelo por defecto: `gemini-3.5-flash-lite` (rápido, con
nivel gratuito); se puede cambiar en la app.

Precios de la página oficial en octubre de 2026 (USD por millón de tokens, nivel de pago):

| Modelo | Entrada | Salida |
|---|---|---|
| `gemini-3.5-flash-lite` | 0,30 | 2,50 |
| `gemini-3.1-flash-lite` | 0,25 | 1,50 |
| `gemini-3.8-flash` | 0,75 | 3,75 (sube el 1 de enero de 2027) |

Los tres tienen nivel gratuito. **En el nivel gratuito, Google usa el contenido para mejorar sus
productos**; en el de pago, no. La app lo dice antes de pedir el consentimiento.

## Cómo decide la app

[`SelectorInterprete`](../interpreter/api/src/main/kotlin/com/lhansqw/senaslsc/interpreter/SelectorInterprete.kt),
en cada frase:

| Preferencia | 1.º | 2.º | 3.º |
|---|---|---|---|
| Automático (por defecto) | IA del teléfono, si está lista | Gemini, si está permitido y hay conexión | Reglas |
| Preferir Gemini | Gemini, si está permitido y hay conexión | IA del teléfono | Reglas |
| Nunca usar internet | IA del teléfono | Reglas | — |

**Sin consentimiento nunca se usa internet**, sea cual sea la preferencia (lo comprueba una prueba
para todas las combinaciones).

## Garantías

- Se envía solo texto: glosa, significado, tipo y confianza de cada seña. Nunca imágenes, vídeo ni
  puntos de referencia.
- La respuesta de Gemini pasa por el **mismo validador** que la del teléfono: si añade señas que no
  se hicieron, omite alguna sin decirlo o no marca una seña dudosa, se descarta y se muestra la
  frase por reglas con el motivo.
- La clave de API se guarda **cifrada** (AES-GCM, clave del Android Keystore), va en la cabecera
  `x-goog-api-key` (nunca en la URL), no se muestra ni se registra, y queda fuera de las copias de
  seguridad. «Borrar clave» la elimina.
- Sin conexión, la app no lo intenta: usa la IA del teléfono o las reglas.

## Comparativa (calidad, latencia y coste)

En el PC, con tu clave en una variable de entorno (no se guarda en ningún archivo):

```powershell
cd C:\dev\senas-lsc
$env:GEMINI_API_KEY = "tu-clave"
.\gradlew.bat :interpreter:remoto:compararGemini
# otro modelo:  .\gradlew.bat :interpreter:remoto:compararGemini -PmodeloGemini=gemini-3.1-flash-lite
```

Envía 10 secuencias con señas del vocabulario de LSC70 y escribe
`interpreter\remoto\build\comparativa\comparativa-gemini.md` con:

- cuántas respuestas cumplen el contrato y cuántas fallan;
- latencia p50 y p95;
- tokens medios por frase y coste estimado cada 1000 frases;
- la frase por reglas y la de Gemini, una al lado de la otra, **para que una persona juzgue la
  calidad** (no se puntúa automáticamente).

La IA del teléfono (Gemma con LiteRT-LM) solo funciona en Android: su latencia se ve en el
teléfono al usarla.

## En la app

Pantalla «IA para proponer frases» → «IA en internet (Gemini)»:

1. «Activar…» muestra qué se envía y a quién; hay que pulsar «Acepto».
2. Pega la clave (de [aistudio.google.com/apikey](https://aistudio.google.com/apikey)) y pulsa
   «Guardar y probar»: envía «HOLA YO» y dice si funciona o por qué no.
3. Elige cuándo usarla.

## Archivos

- `interpreter/remoto/`: `ClienteGemini.kt`, `Comparativa.kt` y sus pruebas (servidor HTTP local).
- `interpreter/api/.../SelectorInterprete.kt` y su prueba.
- `app/.../ia/`: `PreferenciasIaRemota.kt`, `ControladorIaRemota.kt`, `Conectividad.kt`, `TarjetaInternet.kt`.
