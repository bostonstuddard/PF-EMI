# pf.14 verification notes (October 29, 2026)

**ProjectFlint source:** GitHub HEAD `169797e5440474457fd3410456bf43401214bf3c`, `mod_version=0.31.50-b` (checked through GitHub). Inspected latest CHANGELOG and gameplay paths for the torch/bucket/crafting mechanics.

**Status:** Source-level patch and tests only. No Gradle compilation or Minecraft runtime tests have passed here. The local Linux wrapper from the Windows source contains CRLF characters; the required Gradle distribution is unavailable locally, and this sandbox cannot resolve its remote distribution host. Do not mistake test checks for runnable builds.

**UI review scenarios:** shrinking/breaking notes; visible readable contrast in default theme; empty-arrow badge removed; 3×3 time; mouse hover & keyboard A/U on multiblock rails.

**Recipe parity:** personal vs Hardwood grid routing through the active PF recipe manager, Fat Torch, Hardwood Table tool, alternative axes, milking, water buckets and grind-only Utility Vest.

---

# pf.13 — Category routing regression report

**Source reference:** ProjectFlint `0.31.48-b`, commit `6f45b748eb808e50f9945ee174f9ccf255362862`, rechecked October 28, 2026. Game version 1.21.11 / Fabric.

**Status:** Source update and static validation complete. **Compilation and runtime verification NOT complete.**

## Checked / fixed

- Verified `PersonalCraftingGuideRegistry`, selected portable recipe JSONs, six Hardwood shaped JSONs, `SandingStationRecipeRegistry`, `WoodenGrinderRecipeRegistry` and menu bucket return, `PrimitiveStoveRecipeRegistry`, `PrimitiveProcessingRegistry`, `BrickFurnaceRecipeRegistry`, `BasinBlockEntity`, and `FireclayForgeBlockEntity` via source inspection.
- Moved the previously mismatched pf.12 catch-all entries into six accurate categories and added dedicated 2x2 / 3x3 grids with English category names.
- Filled selected missing processing: 25 Sanding dye conversions, Wooden/Clay grinder inheritance, Oven direct foods, Brick Furnace exclusive outputs, upgraded Brush, Pumpkin Cake slicing and bucket byproducts.
- `python tools/verify_pf_emi_pf13.py`: 30/30 static checks pass.
- Java parse-level delimiter checks pass; source files inspected for duplicate category registration and source-recipe IDs.

## Not checked / known limitations

- `./gradlew --offline :fabric:compileJava` fails before compilation because the Gradle 9.2.1 wrapper distribution must be downloaded, but DNS resolution for services.gradle.org is unavailable. No compile success claimed.
- GUI rendering, grid presentation, resource reload, recipe-tree behavior and mixed-loader stability not tested.
- Comprehensive future ProjectFlint gameplay parity and conditions are not certified; audit remains open.
- Native dynamic Leather Repair recipe is preserved under the generic Crafting category pending a component-aware, damage-accurate custom view.
- Only specific ProjectFlint native crafting IDs are remapped; no claim is made that all Minecraft vanilla 3x3 recipes have been verified under the Hardwood table.
- Some earlier pf.12 recipe descriptions are still approximate and require in-game condition validation.

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

