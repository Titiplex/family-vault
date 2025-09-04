#!/usr/bin/env bash
set -euo pipefail
mvn -q -DskipTests package
APPNAME="FamilyHub"
DATA="$HOME/Library/Application Support/FamilyHub"
mkdir -p "$DATA"

jpackage \
  --type dmg \
  --name "$APPNAME" \
  --app-version 1.0.0 \
  --input target \
  --main-jar family-hub-1.0.0.jar \
  --main-class org.tsumiyoku.familyhub.Main \
  --dest target/installer \
  --icon src/main/resources/installer/icon.png \
  --java-options "-Dfamilyhub.dataDir=$DATA"

echo "DMG prêt: target/installer/$APPNAME-1.0.0.dmg"