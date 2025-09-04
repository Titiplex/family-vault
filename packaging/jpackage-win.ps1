# Usage: powershell -ExecutionPolicy Bypass -File packaging/jpackage-win.ps1
$mvn = "mvn"
& $mvn -q -DskipTests package

$APPNAME = "FamilyHub"
$DATA = "$env:LOCALAPPDATA\FamilyHub"
if (!(Test-Path $DATA)) { New-Item -ItemType Directory -Force -Path $DATA | Out-Null }

jpackage `
  --type msi `
  --name $APPNAME `
  --app-version 1.0.0 `
  --input target `
  --main-jar family-hub-1.0.0.jar `
  --main-class org.tsumiyoku.familyhub.Main `
  --dest target\installer `
  --icon src\main\resources\installer\icon.png `
  --win-shortcut `
  --win-menu `
  --java-options "-Dfamilyhub.dataDir=$DATA"
Write-Host "MSI prêt: target\\installer\\$APPNAME-1.0.0.msi"