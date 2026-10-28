#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

# Single Linux entry point: ./build.sh [--forge-only]
case "${1:-}" in
  '') FORGE_ONLY=false ;;
  --forge-only) FORGE_ONLY=true ;;
  *) echo '[PF-EMI] Usage: ./build.sh [--forge-only]' >&2; exit 2 ;;
esac

command -v java >/dev/null || { echo '[PF-EMI] ERROR: Java 25 required.' >&2; exit 2; }
JAVA_MAJOR="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/.*java.specification.version = //p' | head -n1)"
[[ "$JAVA_MAJOR" == 25 ]] || { echo "[PF-EMI] ERROR: Java 25 required; found $JAVA_MAJOR." >&2; exit 2; }
[[ -f gradlew && -f forge/gradlew ]] || { echo '[PF-EMI] ERROR: Missing Gradle wrapper.' >&2; exit 2; }
mkdir -p release
shopt -s nullglob

build_forge() {
  rm -f 'release/1.1+26.3+forge.jar' 'release/1.0+26.3+forge.jar'
  echo '[PF-EMI] Building Forge with standalone ForgeGradle...'
  (cd forge && ./gradlew --no-configuration-cache --console=plain clean build)
  local matches=(forge/build/libs/*+forge.jar)
  [[ ${#matches[@]} -eq 1 ]] || { echo '[PF-EMI] ERROR: Forge release JAR missing or ambiguous.' >&2; return 1; }
  [[ $(wc -c < "${matches[0]}") -ge 100000 ]] || { echo '[PF-EMI] ERROR: Forge JAR is too small.' >&2; return 1; }
  cp "${matches[0]}" 'release/1.1+26.3+forge.jar'
  echo '[PF-EMI] Forge output: release/1.1+26.3+forge.jar'
}

if [[ "$FORGE_ONLY" != true ]]; then
  rm -f 'release/1.1+26.3+fabric.jar' 'release/1.1+26.3+neoforge.jar' \
        'release/1.0+26.3+fabric.jar' 'release/1.0+26.3+neoforge.jar'
  echo '[PF-EMI] Building Fabric and NeoForge 26.3...'
  ./gradlew --console=plain -PmcTarget=26.3 clean printPortTarget :fabric:build :neoforge:build :fabric:shadowJar :neoforge:shadowJar
  fab=(fabric/build/libs/*+fabric.jar)
  neo=(neoforge/build/libs/*+neoforge.jar)
  [[ ${#fab[@]} -eq 1 && ${#neo[@]} -eq 1 ]] || { echo '[PF-EMI] ERROR: Loader release JARs missing or ambiguous.' >&2; exit 1; }
  [[ $(wc -c < "${fab[0]}") -ge 100000 && $(wc -c < "${neo[0]}") -ge 100000 ]] || { echo '[PF-EMI] ERROR: Loader JAR is too small.' >&2; exit 1; }
  cp "${fab[0]}" 'release/1.1+26.3+fabric.jar'
  cp "${neo[0]}" 'release/1.1+26.3+neoforge.jar'
fi
build_forge
echo '[PF-EMI] Finished. Install only the matching loader JAR from release/.'
