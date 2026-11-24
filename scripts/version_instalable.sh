#!/bin/sh
# Genera la versión instalable de Señas LSC (fase 12): pruebas, APK y AAB firmados, comprobados.
# Versión para macOS y Linux de version_instalable.ps1.
#
#   1. Comprueba que hay firma (keystore.properties) y modelo de señas en la app.
#   2. Ejecuta todas las pruebas.
#   3. Compila el APK (para instalar directamente) y el AAB (para Google Play) de producción.
#   4. Verifica la firma con apksigner y copia los archivos a dist/ con su SHA-256.
#
#     ./scripts/version_instalable.sh [--sin-r8] [--sin-pruebas]
#
#   --sin-r8       Compilar sin R8 (para descartar que R8 sea la causa de un fallo).
#   --sin-pruebas  Saltar las pruebas (no recomendado).
set -eu

SIN_R8=0
SIN_PRUEBAS=0
for a in "$@"; do
    case "$a" in
        --sin-r8) SIN_R8=1 ;;
        --sin-pruebas) SIN_PRUEBAS=1 ;;
        *) echo "Opción desconocida: $a" >&2; exit 2 ;;
    esac
done

cd "$(dirname "$0")/.."

fallar() { echo "Error: $*" >&2; exit 1; }
paso() { printf '\n=== %s ===\n' "$1"; }
gradle() { ./gradlew "$@" || fallar "Falló: gradlew $*"; }
propiedad() {
    [ -f "$1" ] || return 0
    sed -n "s/^[[:space:]]*$2[[:space:]]*=[[:space:]]*//p" "$1" | head -n 1
}
campo_json() { sed -n "s/^  \"$2\": *\"\([^\"]*\)\".*/\1/p" "$1" | head -n 1; }
sha256() {
    if command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | cut -d ' ' -f 1
    else sha256sum "$1" | cut -d ' ' -f 1; fi
}

paso "1. Comprobaciones"
[ -f keystore.properties ] || fallar "Falta keystore.properties. Crea la firma una vez con: ./scripts/crear_firma.sh"
ASSETS="recognizer/litert/src/main/assets"
for f in reconocedor_lsc.tflite modelo.json; do
    [ -f "$ASSETS/$f" ] || fallar "Falta $ASSETS/$f. Entrena e instala el modelo con training/entrenar_lsc70.sh."
done
VERSION="$(propiedad gradle.properties senas.versionName)"
CODIGO="$(propiedad gradle.properties senas.versionCode)"
echo "Versión $VERSION (versionCode $CODIGO)"
echo "Modelo de señas: $(campo_json "$ASSETS/modelo.json" nombre) $(campo_json "$ASSETS/modelo.json" version)"

if [ "$SIN_PRUEBAS" -eq 0 ]; then
    paso "2. Pruebas"
    gradle test
fi

paso "3. Compilar APK y AAB de producción"
if [ "$SIN_R8" -eq 1 ]; then
    gradle assembleRelease bundleRelease -Psenas.r8=false
else
    gradle assembleRelease bundleRelease
fi

APK="app/build/outputs/apk/release/app-release.apk"
AAB="app/build/outputs/bundle/release/app-release.aab"
[ -f "$APK" ] || fallar "No se generó $APK (¿falta la firma?)."

paso "4. Verificar la firma"
# En local.properties, «:» y «\» van escapados.
SDK="$(propiedad local.properties sdk.dir | sed 's/\\:/:/g; s/\\\\/\\/g')"
[ -n "$SDK" ] || SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -n "$SDK" ] || SDK="$HOME/Library/Android/sdk"
APKSIGNER=""
if [ -d "$SDK/build-tools" ]; then
    ULTIMA="$(ls "$SDK/build-tools" | sort -t . -k 1,1n -k 2,2n -k 3,3n | tail -n 1)"
    [ -n "$ULTIMA" ] && [ -x "$SDK/build-tools/$ULTIMA/apksigner" ] && APKSIGNER="$SDK/build-tools/$ULTIMA/apksigner"
fi
if [ -n "$APKSIGNER" ]; then
    "$APKSIGNER" verify --print-certs "$APK" || fallar "La firma del APK no es válida."
else
    echo "No se encontró apksigner en $SDK/build-tools: se omite la verificación."
fi

paso "5. Copiar a dist/"
mkdir -p dist
rm -f dist/SHA256SUMS.txt
DEST_APK="dist/senas-lsc-$VERSION.apk"
DEST_AAB="dist/senas-lsc-$VERSION.aab"
cp -f "$APK" "$DEST_APK"
[ -f "$AAB" ] && cp -f "$AAB" "$DEST_AAB"
for f in "$DEST_APK" "$DEST_AAB"; do
    [ -f "$f" ] || continue
    BYTES="$(wc -c < "$f" | tr -d ' ')"
    MB="$(awk "BEGIN { printf \"%.1f\", $BYTES / 1048576 }")"
    HASH="$(sha256 "$f")"
    echo "$f  $MB MB  SHA-256 $HASH"
    echo "$HASH  $(basename "$f")" >> dist/SHA256SUMS.txt
done
echo
echo "Listo. Instala en un teléfono conectado con: adb install -r $DEST_APK"
echo "Después sigue la lista de comprobación de docs/fase-12-version-instalable.md."
