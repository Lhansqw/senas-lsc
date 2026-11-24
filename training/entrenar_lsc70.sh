#!/bin/sh
# Entrena el reconocedor de señas con LSC70 y lo instala en la app, de principio a fin.
# Versión para macOS y Linux de entrenar_lsc70.ps1.
#
#   1. Crea el entorno de Python (.venv) e instala las dependencias, si hace falta.
#   2. Extrae los puntos de MediaPipe de LSC70AN (alfabeto y números) y LSC70W (palabras).
#      Se puede interrumpir: al volver a ejecutarlo continúa donde iba.
#      Si está data/LSC50/LANDMARKS.zip (descargar_lsc50.sh --filtro LANDMARKS), importa también
#      LSC50 y entrena con vocabulario.lsc.json (91 señas).
#   3. Entrena, evalúa con personas que el modelo no vio y exporta el .tflite y modelo.json.
#   4. Copia el modelo a la app (recognizer/litert/src/main/assets).
#
#     cd training
#     ./entrenar_lsc70.sh [opciones]
#
#   --version 0.1.0       Versión del modelo que se escribe en modelo.json.
#   --epocas 80
#   --solo-extraer        Solo extraer los puntos (lo más lento), sin entrenar.
#   --sin-instalar        No copiar el modelo a la app al terminar.
#   --arquitectura gru    gru (por defecto), conv o transformer. Para compararlas: comparar_lsc70.sh.
#   --cuantizacion X      ninguna (por defecto), float16 o int8. Mira antes outputs/.../optimizacion.md.
set -eu

VERSION="0.1.0"
EPOCAS=80
SOLO_EXTRAER=0
SIN_INSTALAR=0
ARQUITECTURA="gru"
CUANTIZACION="ninguna"
while [ $# -gt 0 ]; do
    case "$1" in
        --version) VERSION="$2"; shift ;;
        --epocas) EPOCAS="$2"; shift ;;
        --solo-extraer) SOLO_EXTRAER=1 ;;
        --sin-instalar) SIN_INSTALAR=1 ;;
        --arquitectura) ARQUITECTURA="$2"; shift ;;
        --cuantizacion) CUANTIZACION="$2"; shift ;;
        *) echo "Opción desconocida: $1" >&2; exit 2 ;;
    esac
    shift
done
case "$ARQUITECTURA" in gru|conv|transformer) ;; *) echo "Arquitectura no válida: $ARQUITECTURA" >&2; exit 2 ;; esac
case "$CUANTIZACION" in ninguna|float16|int8) ;; *) echo "Cuantización no válida: $CUANTIZACION" >&2; exit 2 ;; esac

export PYTHONUTF8=1
export TF_CPP_MIN_LOG_LEVEL=2
cd "$(dirname "$0")"

DATOS="data/LSC70"
PUNTOS="data/puntos"
SALIDA="outputs/lsc70-v$VERSION"
VOCABULARIO="vocabulario.lsc70.json"
NOMBRE="reconocedor-lsc70"
CITA="LSC70 (Universidad del Cauca, 2024), CC BY 4.0, doi:10.17632/9ssyn8tff5.2"
LSC50="data/LSC50/LANDMARKS.zip"
VIGILAR="val_loss"
if [ -f "$LSC50" ]; then
    # Con LSC50 la pérdida de validación toca fondo en la época 2: se vigila la exactitud.
    VIGILAR="val_accuracy"
    SALIDA="outputs/lsc-v$VERSION"
    VOCABULARIO="vocabulario.lsc.json"
    NOMBRE="reconocedor-lsc"
    CITA="$CITA; LSC50 (Universidad de los Andes, 2024), CC BY 4.0, doi:10.6084/m9.figshare.27383016"
fi

PYTHON=".venv/bin/python"
fallar() { echo "Error: $*" >&2; exit 1; }
paso() { printf '\n=== %s ===\n' "$1"; }
ejecutar() { "$PYTHON" "$@" || fallar "Falló: python $*"; }

for parte in LSC70AN LSC70W; do
    [ -d "$DATOS/$parte" ] || fallar "Falta $DATOS/$parte. Descomprime LSC70 en training/data/LSC70 (ver DATOS.md)."
done

paso "1. Entorno de Python"
if [ ! -x "$PYTHON" ]; then
    # Busca Python 3.13 o 3.12 (Homebrew instala python3.13 / python3.12).
    EXE=""
    for c in python3.13 python3.12 python3; do
        command -v "$c" >/dev/null 2>&1 || continue
        VER="$("$c" -c 'import sys; print(f"{sys.version_info[0]}.{sys.version_info[1]}")')"
        case "$VER" in 3.12|3.13) EXE="$c"; break ;; esac
    done
    [ -n "$EXE" ] || fallar "Necesitas Python 3.12 o 3.13. En una Mac: brew install python@3.12"
    echo "Creando .venv con $EXE"
    "$EXE" -m venv .venv || fallar "No se pudo crear .venv"
fi
ejecutar -m pip install --disable-pip-version-check -q -r requirements.txt
ejecutar -c "import tensorflow, mediapipe, ai_edge_litert; print('Dependencias listas: TensorFlow', tensorflow.__version__, '· MediaPipe', mediapipe.__version__)"

paso "2. Extraer puntos (la primera vez tarda; se puede interrumpir y continuar)"
ejecutar extraer.py --raiz "$DATOS/LSC70AN" --vocabulario "$VOCABULARIO" --salida "$PUNTOS" --fuente LSC70AN
ejecutar extraer.py --raiz "$DATOS/LSC70W" --vocabulario "$VOCABULARIO" --salida "$PUNTOS" --fuente LSC70W
# Grabaciones hechas con la app (herramienta de grabación): «sin seña» y repeticiones extra.
if [ -d data/grabaciones ]; then
    ejecutar importar_grabaciones.py --entrada data/grabaciones --salida "$PUNTOS" --vocabulario "$VOCABULARIO"
fi
# LSC50 ya trae los puntos de MediaPipe: se leen del zip sin descomprimir.
if [ -f "$LSC50" ]; then
    ejecutar importar_lsc50.py --zip "$LSC50" --salida "$PUNTOS" --vocabulario "$VOCABULARIO"
fi
if [ "$SOLO_EXTRAER" -eq 1 ]; then
    printf '\nPuntos extraídos en %s.\n' "$PUNTOS"
    exit 0
fi

paso "3. Entrenar"
ejecutar entrenar.py --muestras "$PUNTOS" --vocabulario "$VOCABULARIO" --salida "$SALIDA" --epocas "$EPOCAS" --arquitectura "$ARQUITECTURA" --vigilar "$VIGILAR"

paso "4. Evaluar con personas no vistas"
ejecutar evaluar.py --entrenamiento "$SALIDA" --muestras "$PUNTOS"

paso "5. Medir la cuantización"
ejecutar optimizar.py --entrenamiento "$SALIDA" --muestras "$PUNTOS"

paso "6. Exportar"
if [ "$SIN_INSTALAR" -eq 1 ]; then
    ejecutar exportar.py --entrenamiento "$SALIDA" --muestras "$PUNTOS" --vocabulario "$VOCABULARIO" \
        --nombre "$NOMBRE" --version "$VERSION" --datos "$CITA" --cuantizacion "$CUANTIZACION"
else
    ejecutar exportar.py --entrenamiento "$SALIDA" --muestras "$PUNTOS" --vocabulario "$VOCABULARIO" \
        --nombre "$NOMBRE" --version "$VERSION" --datos "$CITA" --cuantizacion "$CUANTIZACION" --instalar
    # La ficha del modelo instalado en la app (fase 12), con las cifras recién medidas.
    ejecutar ficha.py --entrenamiento "$SALIDA" --salida ../docs/ficha-modelo.md
fi

printf '\nListo. Revisa %s/informe.md antes de usar el modelo.\n' "$SALIDA"
[ "$SIN_INSTALAR" -eq 1 ] || echo "Después, en la raíz del proyecto: ./gradlew installDebug"
