# Ficha del modelo: reconocedor-lsc70 0.2.1

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
- Calibración: temperatura 1.462, umbral de energía -8.661.
- Tamaño del archivo `.tflite`: 317 KB.

## Resultados con personas que el modelo no vio

Personas de prueba: Per07, Per08, Per16, Per17, Per19, Per23, Per40, Per43, Per54, Per67 (469 muestras).

| Métrica | Valor |
|---|---|
| Exactitud | 88.5 % |
| F1 macro | 88.3 % |
| Error de calibración (ECE) | 0.027 |
| Señas conocidas rechazadas por el umbral | 1.1 % |

### Señas con peor resultado

| Seña | F1 | Recall | Muestras |
|---|---|---|---|
| S | 62.5 % | 50.0 % | 10 |
| LICOR | 66.7 % | 70.0 % | 10 |
| 6 | 66.7 % | 50.0 % | 10 |
| 1 | 71.4 % | 100.0 % | 10 |
| G | 75.0 % | 60.0 % | 10 |

### Versiones cuantizadas

| Versión | Tamaño | Exactitud |
|---|---|---|
| float32 | 1133 KB | 88.5 % |
| float16 | 582 KB | 88.5 % |
| int8 | 317 KB | 88.5 % |

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
