#!/usr/bin/env bash
set -euo pipefail
mvn -q -DskipTests package
APPNAME="FamilyHub"
DATA="$HOME/.local/share/familyhub"
mkdir -p "$DATA"

# Choisis 'deb' (Debian/Ubuntu) ou 'rpm' selon ta distro
TYPE="${1:-deb}"

jpackage \
  --type "$TYPE" \
  --name "$APPNAME" \
  --app-version 1.0.0 \
  --input target \
  --main-jar family-hub-1.0.0.jar \
  --main-class org.tsumiyoku.familyhub.Main \
  --dest target/installer \
  --icon src/main/resources/installer/icon.png \
  --linux-shortcut \
  --java-options "-Dfamilyhub.dataDir=$DATA"

echo "Paquet prêt: target/installer/$APPNAME-1.0.0.$TYPE"