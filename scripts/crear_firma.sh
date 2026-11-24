#!/bin/sh
# Crea la clave con la que se firma la versión de producción de Señas LSC (fase 12). Una sola vez.
# Versión para macOS y Linux de crear_firma.ps1.
#
# Genera un almacén de claves (.jks) FUERA del repositorio y escribe keystore.properties en la
# raíz del proyecto (que git ignora). Después, ./gradlew assembleRelease firma el APK.
#
# IMPORTANTE: guarda una copia del .jks y de la contraseña en un lugar seguro. Si se pierden, no
# se podrá publicar una actualización de la app que se instale encima de la anterior.
#
#     ./scripts/crear_firma.sh [destino.jks] [alias] [nombre]
set -eu

DESTINO="${1:-$HOME/senas-lsc-firma/senas-lsc.jks}"
ALIAS="${2:-senas-lsc}"
NOMBRE="${3:-Lhansqw}"

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
PROPIEDADES="$RAIZ/keystore.properties"

fallar() { echo "Error: $*" >&2; exit 1; }

[ -e "$DESTINO" ] && fallar "Ya existe $DESTINO. No se sobrescribe: si quieres otra clave, pasa otro destino."
[ -e "$PROPIEDADES" ] && fallar "Ya existe keystore.properties. Bórralo solo si de verdad quieres cambiar de clave."

# keytool viene con el JDK de Android Studio.
KEYTOOL=""
for c in \
    "${JAVA_HOME:-}/bin/keytool" \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" \
    "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" \
    "/opt/android-studio/jbr/bin/keytool"; do
    if [ -x "$c" ]; then KEYTOOL="$c"; break; fi
done
if [ -z "$KEYTOOL" ] && [ -x /usr/libexec/java_home ] && JH="$(/usr/libexec/java_home 2>/dev/null)"; then
    [ -x "$JH/bin/keytool" ] && KEYTOOL="$JH/bin/keytool"
fi
if [ -z "$KEYTOOL" ] && command -v keytool >/dev/null 2>&1; then KEYTOOL="$(command -v keytool)"; fi
[ -n "$KEYTOOL" ] || fallar "No se encontró keytool. Instala Android Studio o define JAVA_HOME."

leer_contrasena() {
    printf '%s: ' "$1" >&2
    stty -echo
    IFS= read -r valor || true
    stty echo
    printf '\n' >&2
    printf '%s' "$valor"
}

CLAVE="$(leer_contrasena "Contraseña para la clave (mínimo 8 caracteres)")"
[ "${#CLAVE}" -ge 8 ] || fallar "La contraseña debe tener al menos 8 caracteres."
[ "$(leer_contrasena "Repite la contraseña")" = "$CLAVE" ] || fallar "Las contraseñas no coinciden."

mkdir -p "$(dirname "$DESTINO")"
# PKCS12 (el formato por defecto) usa la misma contraseña para el almacén y la clave.
"$KEYTOOL" -genkeypair -v -keystore "$DESTINO" -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
    -storepass "$CLAVE" -keypass "$CLAVE" -dname "CN=$NOMBRE, O=Senas LSC, C=CO" \
    || fallar "keytool no pudo crear la clave."

# El archivo lleva la contraseña: solo lo puede leer el usuario.
umask 077
{
    echo "# Firma de la versión de producción. NO subir a git (está en .gitignore)."
    echo "storeFile=$DESTINO"
    echo "storePassword=$CLAVE"
    echo "keyAlias=$ALIAS"
    echo "keyPassword=$CLAVE"
} > "$PROPIEDADES"

echo
echo "Clave creada en $DESTINO"
echo "keystore.properties escrito en $PROPIEDADES"
echo "Haz ahora una copia de seguridad de $DESTINO y de la contraseña."
