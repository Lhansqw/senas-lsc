# Ficha del modelo: reconocedor-lsc70 0.1.0

Generada por `training/ficha.py` a partir de los archivos del entrenamiento. No editar a mano.

## Para qué sirve

Reconoce 42 señas aisladas de la Lengua de Señas Colombiana (LSC) a partir de los
puntos de MediaPipe de manos y cuerpo, en ventanas de 1000 ms. Lo usa la app Señas LSC para mostrar las señas como texto.

**No sirve para** traducir conversaciones en LSC, reconocer señas fuera de su vocabulario ni tomar
decisiones sobre personas. Siempre muestra sus resultados como propuesta revisable.

## Datos

- LSC70 (Universidad del Cauca, 2024), CC BY 4.0, doi:10.17632/9ssyn8tff5.2
- Personas: 50 de entrenamiento, 10 de validación y 10 de prueba. Ninguna persona está en dos grupos.

## Modelo

- Arquitectura: gru
- Entrada: 32 instantes × 352 características (versión 1); salida: logits de 42 clases.
- Usa el rostro: no.
- Calibración: temperatura 1.574, umbral de energía -9.394.
- Tamaño del archivo `.tflite`: 1130 KB.

## Resultados con personas que el modelo no vio

Personas de prueba: Per07, Per08, Per16, Per17, Per19, Per23, Per40, Per43, Per54, Per67 (420 muestras).

| Métrica | Valor |
|---|---|
| Exactitud | 90.7 % |
| F1 macro | 90.6 % |
| Error de calibración (ECE) | 0.029 |
| Señas conocidas rechazadas por el umbral | 1.4 % |

### Señas con peor resultado

| Seña | F1 | Recall | Muestras |
|---|---|---|---|
| S | 47.1 % | 40.0 % | 10 |
| 1 | 72.0 % | 90.0 % | 10 |
| 6 | 73.7 % | 70.0 % | 10 |
| H | 76.2 % | 80.0 % | 10 |
| G | 77.8 % | 70.0 % | 10 |

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
