# PF-EMI pf.12 — verification and unresolved acceptance criteria

**Upstream:** ProjectFlint `0.31.48-b` / Minecraft `1.21.11`, GitHub main `6f45b748eb808e50f9945ee174f9ccf255362862`, rechecked 2026-10-28.

**Status:** This is a source patch. The complete per-item checklist remains **open**, not 100% verified. Some code contracts were directly verified against upstream source through the GitHub connector. A full remote tree checkout is not accessible from this container; no in-game test instance is available.

### Changes with directly inspected upstream source

1. Recipe Parchment: special `ProjectFlintRecipe` custom-data tag and optional `incomplete` CustomModelData on staged Bacon & Eggs.
2. Bookshelf: four inserted Books, blank parchment for initial research, and staged unfinished bacon parchment for the final research.
3. Primitive Oven: bound-specific parchment, 300-tick oven processes and current food-input/count definitions.
4. Wooden Grinder: leather armor 512-max-damage results, alternate Unfired Clay Bucket, existing recent recipes.
5. Clay Grinder: block conversions/armor/fired-brick shapes, compared with `ClayGrinderRecipeGuideRegistry` source.
6. Permanent Campfire: all BOTH-processor heat recipes from `PrimitiveProcessingRegistry`, including buckets/bricks.
7. Turtle: one meat drop for adult, raw or cooked according to on-fire state.
8. Well and Fireclay Forge: source validator geometry and block counts checked against the existing models.

### Missing evidence for acceptance

- No successful Java/Gradle compile here; Gradle 9.2.1 could not be fetched (network DNS unavailable).
- No GUI, multiblock rotation/scale or real world/recipe gameplay tests.
- No full GitHub source tree, user gameplay sources, tags, crafting JSONs, loot tables or dynamic data-manager source parity pass. Individual entries in `PROJECTFLINT_EMI_AUDIT_CHECKLIST.md` that require these remain unchecked.
- Forge and NeoForge remain unclaimed; the current `build.bat` builds Fabric only.
- Source registration existence is not proof of correct runtime output/quantity/conditions. Explicitly validate the modified item-data parchment results on a real client.

### Offline source checks

- `python tools/verify_pf_emi_pf12.py`: **20/20 static assertions PASS**. Does not compile or run Minecraft.


Run `python tools/verify_pf_emi_pf12.py` in the extracted PF-EMI source root. The static script checks the patch's key assertions, duplicate IDs and balanced Java syntax delimiters but does not substitute for `gradlew :fabric:build`.

