#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TARGET="${1:-26.1.2}"
[[ "$TARGET" == '26.1.2' ]] || { echo '[PF-EMI] Forge currently targets only 26.1.2.' >&2; exit 2; }
mkdir -p release
rm -f "release/PF-EMI-${TARGET}-FORGE.jar"
(cd forge && ./gradlew --no-configuration-cache clean build)
shopt -s nullglob
jars=(forge/build/libs/PF-EMI-*.jar)
candidates=()
for jar in "${jars[@]}"; do
  [[ "$jar" == *-sources.jar ]] || candidates+=("$jar")
done
[[ ${#candidates[@]} -eq 1 ]] || { echo '[PF-EMI] Forge output JAR missing or ambiguous.' >&2; exit 1; }
[[ $(wc -c < "${candidates[0]}") -ge 100000 ]] || { echo '[PF-EMI] Forge output JAR is suspiciously small.' >&2; exit 1; }
cp "${candidates[0]}" "release/PF-EMI-${TARGET}-FORGE.jar"
echo "[PF-EMI] Forge output: release/PF-EMI-${TARGET}-FORGE.jar"
