#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

TARGET="${1:-26.1.2}"
if [[ "$TARGET" != '26.1.2' ]]; then
  echo "[PF-EMI] ERROR: This source checkout is for 26.1.2, not $TARGET." >&2
  echo '[PF-EMI] Other version profiles alone do not port the Java source.' >&2
  exit 2
fi
PROFILE="versions/${TARGET}.properties"
[[ -f "$PROFILE" ]] || { echo "[PF-EMI] ERROR: Missing $PROFILE" >&2; exit 2; }
[[ -f gradlew ]] || { echo '[PF-EMI] ERROR: Missing Gradle wrapper.' >&2; exit 2; }
command -v java >/dev/null || { echo '[PF-EMI] ERROR: Java 25 is required.' >&2; exit 2; }
JAVA_MAJOR="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/.*java.specification.version = //p' | head -n1)"
[[ "$JAVA_MAJOR" == 25 ]] || { echo "[PF-EMI] ERROR: Java 25 required; found $JAVA_MAJOR" >&2; exit 2; }

args=("-PmcTarget=$TARGET")
while IFS='=' read -r key value || [[ -n "$key" ]]; do
  [[ -z "$key" || "$key" == \#* ]] && continue
  key="${key%$'\r'}"
  value="${value%$'\r'}"
  args+=("-P${key}=${value}")
done < "$PROFILE"

mkdir -p release
rm -f "release/PF-EMI-${TARGET}.jar" "release/PF-EMI-${TARGET}-NEOFORGE.jar"
printf '[PF-EMI] Building Fabric and NeoForge for Minecraft %s ...\n' "$TARGET"
echo '[PF-EMI] Forge is built in an independent ForgeGradle 7 project.'
./gradlew "${args[@]}" clean printPortTarget :fabric:build :neoforge:build :fabric:shadowJar :neoforge:shadowJar

# shadowJar produces the playable JAR; :jar produces a dev/shared module.
shopt -s nullglob
fabric_jars=(fabric/build/libs/*+fabric.jar)
neo_jars=(neoforge/build/libs/*+neoforge.jar)
[[ ${#fabric_jars[@]} -eq 1 ]] || { echo "[PF-EMI] ERROR: Expected one Fabric release jar, found ${#fabric_jars[@]}." >&2; exit 1; }
[[ ${#neo_jars[@]} -eq 1 ]] || { echo "[PF-EMI] ERROR: Expected one NeoForge release jar, found ${#neo_jars[@]}." >&2; exit 1; }

fabric_bytes=$(wc -c < "${fabric_jars[0]}")
neo_bytes=$(wc -c < "${neo_jars[0]}")
[[ $fabric_bytes -ge 100000 && $neo_bytes -ge 100000 ]] || { echo '[PF-EMI] ERROR: A release jar is unexpectedly tiny; refusing to copy a dev/shared jar.' >&2; exit 1; }

mkdir -p release
cp "${fabric_jars[0]}" "release/PF-EMI-${TARGET}.jar"
cp "${neo_jars[0]}" "release/PF-EMI-${TARGET}-NEOFORGE.jar"
echo '[PF-EMI] Fabric and NeoForge build complete:'
echo "  release/PF-EMI-${TARGET}.jar"
echo "  release/PF-EMI-${TARGET}-NEOFORGE.jar"
echo '[PF-EMI] Building experimental Forge 26.1.2...'
(cd forge && ./gradlew clean build)
forge_jars=(forge/build/libs/PF-EMI-*.jar)
forge_releases=()
for candidate in "${forge_jars[@]}"; do
  [[ "$candidate" == *-sources.jar ]] || forge_releases+=("$candidate")
done
[[ ${#forge_releases[@]} -eq 1 ]] || { echo "[PF-EMI] Expected one Forge release, got ${#forge_releases[@]}." >&2; exit 1; }
[[ $(wc -c < "${forge_releases[0]}") -ge 100000 ]] || { echo '[PF-EMI] Forge release JAR is too small.' >&2; exit 1; }
cp "${forge_releases[0]}" "release/PF-EMI-${TARGET}-FORGE.jar"
echo "  release/PF-EMI-${TARGET}-FORGE.jar"
echo '[PF-EMI] Forge build success does not replace in-game runtime testing.' 
