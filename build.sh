#!/usr/bin/env bash
# =============================================================================
# Hytale Mod Manager — Script de build Linux / macOS
# Génère :  target/hytale-mod-manager.jar           (fat-jar)
#           target/installer/*.deb ou *.dmg          (paquet natif)
# =============================================================================
set -euo pipefail

APP_NAME="Hytale Mod Manager"
APP_VERSION="1.1.0"
APP_VENDOR="Votre pseudo ici"
APP_DESCRIPTION="Gestionnaire et mise à jour des mods Hytale"
ICON_PNG="src/main/resources/com/hytale/modmanager/icon/icon-256.png"
MAIN_CLASS="com.hytale.modmanager.Main"

# Type d'installateur selon l'OS
OS="$(uname -s)"
if [[ "$OS" == "Darwin" ]]; then
    INSTALLER_TYPE="dmg"
elif [[ "$OS" == "Linux" ]]; then
    INSTALLER_TYPE="deb"
else
    INSTALLER_TYPE="app-image"
fi

# Surcharge via argument : ./build.sh rpm  ou  ./build.sh dmg
[[ $# -ge 1 ]] && INSTALLER_TYPE="$1"

echo "============================================================"
echo "  Hytale Mod Manager — Packaging v${APP_VERSION}"
echo "  Plateforme : ${OS}  |  Installateur : ${INSTALLER_TYPE}"
echo "============================================================"

# Vérifications
for cmd in java javac mvn jpackage; do
    command -v "$cmd" >/dev/null 2>&1 || { echo "ERREUR : '$cmd' introuvable."; exit 1; }
done
echo "[OK] Java : $(java -version 2>&1 | head -1)"

# 1. Compilation + fat-jar
echo; echo "[1/3] Compilation Maven..."
mvn clean package -q
echo "[OK] target/hytale-mod-manager.jar créé"

# 2. jpackage
mkdir -p target/installer
echo; echo "[2/3] jpackage (${INSTALLER_TYPE})..."

ICON_OPT="--icon ${ICON_PNG}"
EXTRA_OPTS=""
if [[ "$OS" == "Darwin" ]]; then
    EXTRA_OPTS="--mac-package-name '${APP_NAME}'"
fi

jpackage \
    --type "${INSTALLER_TYPE}" \
    --name "${APP_NAME}" \
    --app-version "${APP_VERSION}" \
    --vendor "${APP_VENDOR}" \
    --description "${APP_DESCRIPTION}" \
    --input target \
    --main-jar hytale-mod-manager.jar \
    --main-class "${MAIN_CLASS}" \
    --dest target/installer \
    ${ICON_OPT} \
    --java-options "-Dfile.encoding=UTF-8" \
    $EXTRA_OPTS

# 3. Résumé
echo; echo "[3/3] Terminé !"
echo "============================================================"
echo "  Fichiers produits :"
echo
echo "  JAR : target/hytale-mod-manager.jar"
echo "  Paquet natif :"
ls -1 target/installer/ | sed 's/^/    /'
echo "============================================================"
