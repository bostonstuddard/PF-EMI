#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
  echo "[PF-EMI] ERROR: Java was not found in PATH. Install Java 21." >&2
  exit 1
fi

echo "[PF-EMI] Java runtime found in PATH:"
java -version

JAVA_HOME_FROM_JAVA="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.home = //p' | head -n1)"
if [[ -z "$JAVA_HOME_FROM_JAVA" || ! -x "$JAVA_HOME_FROM_JAVA/bin/java" ]]; then
  echo "[PF-EMI] ERROR: Could not determine a valid java.home from Java in PATH." >&2
  exit 1
fi

export JAVA_HOME="$JAVA_HOME_FROM_JAVA"
export PATH="$JAVA_HOME/bin:$PATH"

echo
echo "[PF-EMI] Forced JAVA_HOME for Gradle: $JAVA_HOME"
echo "[PF-EMI] Gradle runtime verification:"
./gradlew --version --no-daemon "-Dorg.gradle.java.home=$JAVA_HOME"

echo
echo "[PF-EMI] Building Fabric and NeoForge for Minecraft 1.21.4..."
echo "[PF-EMI] Toolchain: Architectury Plugin 3.4.162 / Loom 1.11.458 / Gradle 8.14 / Java 21"
./gradlew :fabric:clean :fabric:build :neoforge:clean :neoforge:build --no-daemon "-Dorg.gradle.java.home=$JAVA_HOME"

echo
echo "[PF-EMI] Build complete."
echo "[PF-EMI] Fabric jars:   fabric/build/libs/"
echo "[PF-EMI] NeoForge jars: neoforge/build/libs/"
