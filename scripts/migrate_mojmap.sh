#!/usr/bin/env bash
set -euo pipefail
# Run only AFTER: python3 ports/26x/scripts/prepare_port.py
# Use Java 21 with the legacy root Gradle project; Java 25 for the new 26.x project.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
STAGED="$ROOT/ports/26x/src/main/java"
if [[ ! -d "$STAGED" ]] || ! find "$STAGED" -name '*.java' -print -quit | grep -q .; then
	echo 'Run python3 ports/26x/scripts/prepare_port.py first.' >&2
	exit 1
fi
cd "$ROOT"
# Loom migrates the entire combined Yarn source tree in place; originals are stored separately.
./gradlew :xplat:migrateMappings \
	--input "$STAGED" --output "$STAGED" \
	--mappings 'net.minecraft:mappings:1.21.11' \
	--overrideInputsIHaveABackup
# A single combined pass also catches Fabric-specific Yarn symbols; inspect every mixin.
NEO_STAGE="$ROOT/ports/26x/neoforge/src/main/java"
if [[ -d "$NEO_STAGE" ]] && find "$NEO_STAGE" -name '*.java' -print -quit | grep -q .; then
	./gradlew :xplat:migrateMappings --input "$NEO_STAGE" --output "$NEO_STAGE" \
		--mappings 'net.minecraft:mappings:1.21.11' --overrideInputsIHaveABackup
fi
FORGE_STAGE="$ROOT/ports/26x/forge/src/main/java"
if [[ -d "$FORGE_STAGE" ]] && find "$FORGE_STAGE" -name '*.java' -print -quit | grep -q .; then
    ./gradlew :xplat:migrateMappings --input "$FORGE_STAGE" --output "$FORGE_STAGE" \
        --mappings 'net.minecraft:mappings:1.21.11' --overrideInputsIHaveABackup
fi
echo 'Mojang-name conversion attempted. Next: fix 26.x API/loader symbols and access widener.'
