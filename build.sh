#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

MC_VERSION="$(sed -n 's/^minecraft_version=//p' gradle.properties | head -n1 | tr -d '\r')"
[[ -n "$MC_VERSION" ]] || { echo "[PF-EMI] ERROR: Could not read minecraft_version." >&2; exit 1; }
[[ "$MC_VERSION" == "1.21.11" ]] || { echo "[PF-EMI] ERROR: This builder expects Minecraft 1.21.11; found $MC_VERSION." >&2; exit 1; }
command -v java >/dev/null 2>&1 || { echo "[PF-EMI] ERROR: Java 21 is required." >&2; exit 1; }
JAVA_HOME_FROM_JAVA="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.home = //p' | head -n1)"
JAVA_SPEC="$("$JAVA_HOME_FROM_JAVA/bin/java" -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.specification.version = //p' | head -n1)"
[[ "$JAVA_SPEC" == "21" ]] || { echo "[PF-EMI] ERROR: Java 21 is required; found $JAVA_SPEC." >&2; exit 1; }
export JAVA_HOME="$JAVA_HOME_FROM_JAVA"
export PATH="$JAVA_HOME/bin:$PATH"
OUT_DIR="$PWD/release"
rm -rf "$OUT_DIR" && mkdir -p "$OUT_DIR"

echo "[PF-EMI] Target Minecraft version: $MC_VERSION"
echo "[PF-EMI] Loader targets: Fabric, Forge, NeoForge"
./gradlew --version --no-daemon "-Dorg.gradle.java.home=$JAVA_HOME"
rm -rf fabric/build/libs forge/build/libs neoforge/build/libs
./gradlew :fabric:clean :forge:clean :neoforge:clean :fabric:remapJar :forge:remapJar :neoforge:remapJar --no-daemon "-Dorg.gradle.java.home=$JAVA_HOME"

find_final_jar() {
	find "$1/build/libs" -maxdepth 1 -type f -name '*.jar' \
		! -iname '*dev-shadow*' ! -iname '*sources*' ! -iname '*api*' ! -iname '*filtered*' | head -n1
}

FABRIC_JAR="$(find_final_jar fabric)"
FORGE_JAR="$(find_final_jar forge)"
NEOFORGE_JAR="$(find_final_jar neoforge)"
[[ -n "$FABRIC_JAR" ]] || { echo "[PF-EMI] ERROR: Fabric jar missing." >&2; exit 1; }
[[ -n "$FORGE_JAR" ]] || { echo "[PF-EMI] ERROR: Forge jar missing." >&2; exit 1; }
[[ -n "$NEOFORGE_JAR" ]] || { echo "[PF-EMI] ERROR: NeoForge jar missing." >&2; exit 1; }

cp "$FABRIC_JAR" "$OUT_DIR/PF-EMI-$MC_VERSION.jar"
cp "$FORGE_JAR" "$OUT_DIR/PF-EMI-$MC_VERSION-FORGE.jar"
cp "$NEOFORGE_JAR" "$OUT_DIR/PF-EMI-$MC_VERSION-NEOFORGE.jar"
rm -rf fabric/build/libs forge/build/libs neoforge/build/libs

echo "[PF-EMI] BUILD COMPLETE"
echo "[PF-EMI] Fabric:   release/PF-EMI-$MC_VERSION.jar"
echo "[PF-EMI] Forge:    release/PF-EMI-$MC_VERSION-FORGE.jar"
echo "[PF-EMI] NeoForge: release/PF-EMI-$MC_VERSION-NEOFORGE.jar"
