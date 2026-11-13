# Ficha del modelo: reconocedor-lsc70 0.2.0

Generada por `training/ficha.py` a partir de los archivos del entrenamiento. No editar a mano.

## Para qué sirve

Reconoce 47 señas aisladas de la Lengua de Señas Colombiana (LSC) a partir de los
puntos de MediaPipe de manos y cuerpo, en ventanas de 1000 ms. Lo usa la app Señas LSC para mostrar las señas como texto.

**No sirve para** traducir conversaciones en LSC, reconocer señas fuera de su vocabulario ni tomar
decisiones sobre personas. Siempre muestra sus resultados como propuesta revisable.

## Datos

- LSC70 (Universidad del Cauca, 2024), CC BY 4.0, doi:10.17632/9ssyn8tff5.2
- Personas: 50 de entrenamiento, 10 de validación y 10 de prueba. Ninguna persona está en dos grupos.

## Modelo

- Arquitectura: gru  284 271 parámetros
- Entrada: 32 instantes × 352 características (versión 1); salida: logits de 47 clases.
- Usa el rostro: no.
- Calibración: temperatura 1.272, umbral de energía -7.544.
- Tamaño del archivo `.tflite`: 317 KB.

## Resultados con personas que el modelo no vio

Personas de prueba: Per07, Per08, Per16, Per17, Per19, Per23, Per40, Per43, Per54, Per67 (469 muestras).

| Métrica | Valor |
|---|---|
| Exactitud | 87.8 % |
| F1 macro | 87.5 % |
| Error de calibración (ECE) | 0.032 |
| Señas conocidas rechazadas por el umbral | 1.5 % |

### Señas con peor resultado

| Seña | F1 | Recall | Muestras |
|---|---|---|---|
| H | 54.5 % | 60.0 % | 10 |
| 6 | 55.6 % | 50.0 % | 10 |
| S | 57.1 % | 40.0 % | 10 |
| Ñ | 61.5 % | 44.4 % | 9 |
| 1 | 64.3 % | 90.0 % | 10 |

### Versiones cuantizadas

| Versión | Tamaño | Exactitud |
|---|---|---|
| float32 | 1133 KB | 87.8 % |
| float16 | 582 KB | 87.8 % |
| int8 | 317 KB | 87.6 % |

## Limitaciones conocidas

- **No tiene clase «sin seña».** Cualquier movimiento de las manos puede reconocerse como alguna seña; solo el umbral de energía y el filtro temporal de la app lo frenan.
- **No usa el rostro**, así que no distingue señas que solo cambian por la expresión facial o la boca.
- Entrenado con **fotos** (6 por seña) interpoladas, no con vídeo: en la cámara en vivo puede rendir peor.
- Las personas de LSC70 no son expertas en LSC; las señas deben validarse con una persona competente.
- Con 10 personas de prueba, las cifras tienen un margen de varios puntos.
- No se ha medido por separado en personas zurdas, con poca luz ni con fondos complejos.

## Licencia

Los datos de entrenamiento se citan arriba con su licencia; el modelo se distribuye dentro de la app con esa
misma atribución (pantalla «Acerca de»).
