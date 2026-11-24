#!/bin/sh
# Descarga LSC50 (Universidad de los Andes) desde Figshare archivo por archivo, con reanudación.
# Versión para macOS y Linux de descargar_lsc50.ps1.
#
# «Download all» en el navegador genera un único .zip de 9,9 GB que falla si la conexión se corta.
# Este script pide a la API pública de Figshare la lista de archivos y descarga cada uno con curl,
# que continúa donde se quedó. Al terminar comprueba el MD5 de cada archivo con el que publica
# Figshare. Necesita curl y python3 (para leer el JSON de la API).
#
#     cd training
#     ./descargar_lsc50.sh --solo-listar     # ver licencia y archivos
#     ./descargar_lsc50.sh                   # descargar todo
#     ./descargar_lsc50.sh --filtro csv      # solo los que contengan «csv»
#
#   --filtro X       Solo descarga los archivos cuyo nombre contiene este texto.
#   --solo-listar    Muestra la licencia y la lista de archivos sin descargar nada.
#   --destino DIR    Carpeta de destino (por defecto data/LSC50).
set -eu

FILTRO=""
SOLO_LISTAR=0
DESTINO="data/LSC50"
while [ $# -gt 0 ]; do
    case "$1" in
        --filtro) FILTRO="$2"; shift ;;
        --solo-listar) SOLO_LISTAR=1 ;;
        --destino) DESTINO="$2"; shift ;;
        *) echo "Opción desconocida: $1" >&2; exit 2 ;;
    esac
    shift
done

cd "$(dirname "$0")"

PYTHON=""
for c in .venv/bin/python python3; do
    if command -v "$c" >/dev/null 2>&1 || [ -x "$c" ]; then PYTHON="$c"; break; fi
done
[ -n "$PYTHON" ] || { echo "Error: hace falta python3 para leer la respuesta de Figshare." >&2; exit 1; }

ARTICULO=27383016
API="https://api.figshare.com/v2/articles/$ARTICULO"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "Consultando Figshare…"
curl -fsSL "$API" -o "$TMP/info.json"
curl -fsSL "$API/files?page_size=1000" -o "$TMP/archivos.json"

# Lista separada por tabuladores: nombre, tamaño, md5, url.
"$PYTHON" - "$TMP/info.json" "$TMP/archivos.json" "$FILTRO" "$TMP/lista.tsv" <<'PY'
import json, sys
info = json.load(open(sys.argv[1], encoding="utf-8"))
archivos = json.load(open(sys.argv[2], encoding="utf-8"))
filtro = sys.argv[3]
sel = [a for a in archivos if filtro == "" or filtro in a["name"]]
print(f"\n{info['title']}")
print(f"Licencia: {info['license']['name']}  ({info['license']['url']})")
print(f"DOI: {info['doi']}\n")
for a in sel:
    print(f"  {a['name']:<60} {a['size'] / 1048576:10.1f} MB")
total = sum(a["size"] for a in sel)
print(f"\n{len(sel)} archivos, {total / 1073741824:.2f} GB")
with open(sys.argv[4], "w", encoding="utf-8") as f:
    for a in sel:
        md5 = a.get("computed_md5") or a.get("supplied_md5") or "-"
        f.write(f"{a['name']}\t{a['size']}\t{md5}\t{a['download_url']}\n")
with open(sys.argv[4] + ".total", "w") as f:
    f.write(str(total))
PY
[ "$SOLO_LISTAR" -eq 1 ] && exit 0

TOTAL="$(cat "$TMP/lista.tsv.total")"
mkdir -p "$DESTINO"
LIBRE_KB="$(df -Pk "$DESTINO" | awk 'NR == 2 { print $4 }')"
if awk "BEGIN { exit !($LIBRE_KB * 1024 < $TOTAL * 1.1) }"; then
    awk "BEGIN { printf \"Error: no hay espacio suficiente: hacen falta %.1f GB y hay %.1f GB libres.\n\", $TOTAL * 1.1 / 1073741824, $LIBRE_KB / 1048576 }" >&2
    exit 1
fi

md5_de() {
    if command -v md5 >/dev/null 2>&1; then md5 -q "$1"
    else md5sum "$1" | cut -d ' ' -f 1; fi
}
tamano() { wc -c < "$1" | tr -d ' '; }

N_TOTAL="$(wc -l < "$TMP/lista.tsv" | tr -d ' ')"
FALLIDOS=""
N=0
TAB="$(printf '\t')"
while IFS="$TAB" read -r NOMBRE TAMANO MD5 URL; do
    N=$((N + 1))
    RUTA="$DESTINO/$NOMBRE"
    if [ -f "$RUTA" ] && [ "$(tamano "$RUTA")" = "$TAMANO" ]; then
        echo "[$N/$N_TOTAL] Ya estaba: $NOMBRE"
        continue
    fi
    echo "[$N/$N_TOTAL] Descargando $NOMBRE…"
    INTENTOS=0
    OK=0
    while [ "$OK" -eq 0 ] && [ "$INTENTOS" -lt 3 ]; do
        INTENTOS=$((INTENTOS + 1))
        # -C - continúa una descarga a medias; --retry reintenta cortes de red.
        if curl -L --fail --retry 5 --retry-delay 5 -C - -o "$RUTA" "$URL" </dev/null \
            && [ -f "$RUTA" ] && [ "$(tamano "$RUTA")" = "$TAMANO" ]; then
            OK=1
        fi
    done
    if [ "$OK" -eq 0 ]; then FALLIDOS="$FALLIDOS $NOMBRE"; continue; fi

    if [ "$MD5" != "-" ]; then
        CALCULADO="$(md5_de "$RUTA")"
        if [ "$CALCULADO" != "$(echo "$MD5" | tr 'A-F' 'a-f')" ]; then
            echo "  MD5 distinto: se borra y habrá que repetir."
            rm -f "$RUTA"
            FALLIDOS="$FALLIDOS $NOMBRE"
        fi
    fi
done < "$TMP/lista.tsv"

if [ -n "$FALLIDOS" ]; then
    printf '\nNo se pudieron descargar:%s. Vuelve a ejecutar el script: continúa donde quedó.\n' "$FALLIDOS" >&2
    exit 1
fi
printf '\nListo: archivos en %s. Si hay .zip, descomprímelos ahí mismo.\n' "$DESTINO"
