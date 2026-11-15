#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

MC_VERSION="$(sed -n 's/^minecraft_version=//p' gradle.properties | head -n1 | tr -d '\r')"
if [[ -z "$MC_VERSION" ]]; then
  echo "[PF-EMI] ERROR: Could not read minecraft_version from gradle.properties." >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "[PF-EMI] ERROR: Java was not found in PATH. Install Java 21." >&2
  exit 1
fi

JAVA_HOME_FROM_JAVA="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.home = //p' | head -n1)"
if [[ -z "$JAVA_HOME_FROM_JAVA" || ! -x "$JAVA_HOME_FROM_JAVA/bin/java" ]]; then
  echo "[PF-EMI] ERROR: Could not determine a valid java.home from Java in PATH." >&2
  exit 1
fi

JAVA_SPEC="$("$JAVA_HOME_FROM_JAVA/bin/java" -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.specification.version = //p' | head -n1)"
if [[ "$JAVA_SPEC" != "21" ]]; then
  echo "[PF-EMI] ERROR: Java 21 is required; found Java $JAVA_SPEC at $JAVA_HOME_FROM_JAVA." >&2
  exit 1
fi

export JAVA_HOME="$JAVA_HOME_FROM_JAVA"
export PATH="$JAVA_HOME/bin:$PATH"
OUT_DIR="$PWD/release"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

echo "[PF-EMI] Target Minecraft version: $MC_VERSION"
echo "[PF-EMI] Gradle runtime verification:"
./gradlew --version --no-daemon "-Dorg.gradle.java.home=$JAVA_HOME"

echo
echo "[PF-EMI] Building Fabric, Forge, Quilt-compatible, and NeoForge artifacts..."
./gradlew \
  :fabric:clean :forge:clean :neoforge:clean \
  :fabric:remapJar :forge:remapJar :neoforge:remapJar \
  --no-daemon "-Dorg.gradle.java.home=$JAVA_HOME"

pick_jar() {
  local dir="$1"
  find "$dir" -maxdepth 1 -type f -name '*.jar' \
    ! -iname '*dev-shadow*' ! -iname '*sources*' ! -iname '*api*' \
    | head -n1
}

FABRIC_JAR="$(pick_jar fabric/build/libs)"
FORGE_JAR="$(pick_jar forge/build/libs)"
NEOFORGE_JAR="$(pick_jar neoforge/build/libs)"

[[ -n "$FABRIC_JAR" ]] || { echo "[PF-EMI] ERROR: Fabric jar missing." >&2; exit 1; }
[[ -n "$FORGE_JAR" ]] || { echo "[PF-EMI] ERROR: Forge jar missing." >&2; exit 1; }
[[ -n "$NEOFORGE_JAR" ]] || { echo "[PF-EMI] ERROR: NeoForge jar missing." >&2; exit 1; }

cp "$FABRIC_JAR" "$OUT_DIR/PF-EMI-$MC_VERSION.jar"
cp "$FABRIC_JAR" "$OUT_DIR/PF-EMI-$MC_VERSION-QUILT.jar"
cp "$FORGE_JAR" "$OUT_DIR/PF-EMI-$MC_VERSION-FORGE.jar"
cp "$NEOFORGE_JAR" "$OUT_DIR/PF-EMI-$MC_VERSION-NEOFORGE.jar"

rm -rf fabric/build/libs forge/build/libs neoforge/build/libs

echo
echo "[PF-EMI] BUILD COMPLETE"
echo "[PF-EMI] Fabric:   release/PF-EMI-$MC_VERSION.jar"
echo "[PF-EMI] Quilt:    release/PF-EMI-$MC_VERSION-QUILT.jar (Fabric-compatible artifact)"
echo "[PF-EMI] Forge:    release/PF-EMI-$MC_VERSION-FORGE.jar"
echo "[PF-EMI] NeoForge: release/PF-EMI-$MC_VERSION-NEOFORGE.jar"
