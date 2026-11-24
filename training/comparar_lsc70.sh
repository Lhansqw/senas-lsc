#!/bin/sh
# Fase 11: entrena las tres arquitecturas con LSC70 y las compara (GRU, convolucional, Transformer).
# Versión para macOS y Linux de comparar_lsc70.ps1.
#
# Usa los puntos ya extraídos por entrenar_lsc70.sh (data/puntos) y el mismo entorno .venv.
# Para cada arquitectura: entrena, evalúa con las mismas personas de prueba y mide la
# cuantización. Al final escribe outputs/comparacion/comparacion.md. No instala nada en la app.
#
#     cd training
#     ./comparar_lsc70.sh [--epocas 80] [gru conv transformer]
set -eu

EPOCAS=80
ARQUITECTURAS=""
while [ $# -gt 0 ]; do
    case "$1" in
        --epocas) EPOCAS="$2"; shift ;;
        *) ARQUITECTURAS="$ARQUITECTURAS $1" ;;
    esac
    shift
done
[ -n "$ARQUITECTURAS" ] || ARQUITECTURAS="gru conv transformer"

export PYTHONUTF8=1
export TF_CPP_MIN_LOG_LEVEL=2
cd "$(dirname "$0")"

PYTHON=".venv/bin/python"
PUNTOS="data/puntos"
VOCABULARIO="vocabulario.lsc70.json"
RAIZ="outputs/comparacion"

fallar() { echo "Error: $*" >&2; exit 1; }
ejecutar() { "$PYTHON" "$@" || fallar "Falló: python $*"; }

[ -x "$PYTHON" ] || fallar "Falta .venv. Ejecuta primero entrenar_lsc70.sh."
[ -d "$PUNTOS" ] || fallar "Falta $PUNTOS. Ejecuta primero entrenar_lsc70.sh (o con --solo-extraer)."

CARPETAS=""
for a in $ARQUITECTURAS; do
    SALIDA="$RAIZ/$a"
    printf '\n=== %s ===\n' "$a"
    # Misma semilla en todas: misma partición de personas, comparación justa.
    ejecutar entrenar.py --muestras "$PUNTOS" --vocabulario "$VOCABULARIO" --salida "$SALIDA" --arquitectura "$a" --epocas "$EPOCAS" --semilla 42
    ejecutar evaluar.py --entrenamiento "$SALIDA" --muestras "$PUNTOS"
    ejecutar optimizar.py --entrenamiento "$SALIDA" --muestras "$PUNTOS"
    CARPETAS="$CARPETAS $SALIDA"
done

printf '\n=== Comparación ===\n'
# CARPETAS va sin comillas a propósito: una ruta por arquitectura, sin espacios.
# shellcheck disable=SC2086
ejecutar comparar.py $CARPETAS --salida "$RAIZ/comparacion.md"
echo "Tabla en $RAIZ/comparacion.md. Para instalar una: python exportar.py --entrenamiento $RAIZ/<arquitectura> ..."
