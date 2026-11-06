# PF-EMI pf.15 — Verification report

## Upstream

- ProjectFlint v0.31.52-b at commit `37c5f2af2a9939ac71c9d04664954caf10beeb7c`.
- Verified the 0.31.51-b/0.31.52-b release notes, `HardwoodCraftingRecipe`, `ModRecipes`, `DurableCraftingRecipe`, `HardwoodCraftingMenu`, `OreMiningRules`, and changed JSON samples through GitHub.
- GitHub change list contains 126 `hardwood_*` recipe paths, plus separate station-only recipes such as `sticks_from_planks` and `vanilla_wooden_*`.

## Implemented

- Dedicated recipe-type discovery (not output guessing); direct 3x3 source grids; source-backed JSON fallback for absent synchronized recipes.
- Removes stale/retired personal crafting entries; Hardwood Sticks and new station-only recipe pages are discovered automatically.
- Updated ore block-drop explanatory pages and 0.31.52-b release metadata.

## Local checks

- 30/30 offline static assertions passed.
- Two changed Java compilation units parsed with JDK 21 compiler AST; zero syntax errors (this is NOT bytecode compilation).
- Gradle `:fabric:compileJava` was attempted; the Gradle wrapper failed to download Gradle 9.2.1 (`UnknownHostException: services.gradle.org`).

## Validation still required

- Full Java/Gradle compile against the online Fabric/Architectury/Minecraft dependencies.
- Launch Fabric client + ProjectFlint and verify the number of registered Hardwood recipes and UI navigation.
- Test the 60-tick server crafting delay, shaped/shapeless/durable remainders, tooltip contrast and multiblock item-rail interactions.
- Check dedicated-server recipe synchronization vs bundled fallback; test recipe datapack overrides/removals.
- Forge/NeoForge are not supported for this Fabric ProjectFlint update. No 100% parity claim.


## pf.17 — ProjectFlint 0.31.55-b (2026-11-04)

- Verified against ProjectFlint's latest `main` commit `54184ab574bc1999ad616d91518d4681365fca96` and source files described in `PF17_PROJECTFLINT_0.31.55_PARITY.md`.
- 53/53 offline contract checks passed using `python tools/verify_pf_emi_pf17.py`.
- JDK 21 JavaCompiler/JavacTask parsed the two modified Java files with zero syntax diagnostics. This is parsing only, **not compilation** against mod dependencies.
- Gradle 9.2.1 was not cached; wrapper download failed with `UnknownHostException: services.gradle.org`. Consequently no full Fabric build or in-game runtime test is claimed.
- Tested ZIP CRC integrity and required patch-only paths. No full source, Gradle cache, output binaries, or third-party textures bundled.
- Intentionally left runtime and cross-loader checklist entries unchecked.


## pf.18 — ProjectFlint 0.31.57-b (2026-11-05)

- GitHub reference: `7e7321a06daff837866c6ba95c4436f2e83c8824`. Source reviewed: 0.31.57-b release notes, `BasinMaterialProcessingSystem`, `BasinBlockEntity`, JSON bootstrap and glass recipes, and Fireclay Forge processing.
- 62/62 source-contract checks passed using `python tools/verify_pf_emi_pf18.py`; these check selected parity invariants, not all vanilla or mod recipes.
- JDK 21 JavaCompiler AST parsed four new/modified Java files with zero syntax diagnostics. Not dependency-aware bytecode compilation.
- Attempted `:fabric:compileJava` with Gradle wrapper; failed **before compilation** because Gradle 9.2.1 was not cached and `services.gradle.org` could not resolve (`UnknownHostException`).
- No live EMI layout, hover behavior, Basin RNG/hunger, singleplayer or multiplayer tests were possible. Those checks remain outstanding.
- Scope: Fabric 1.21.11 ProjectFlint integration; no Forge/NeoForge claim.


## pf.19 / ProjectFlint v0.31.61-b

- Current authoritative upstream commit was inspected (0.31.61-b), including 0.31.58–0.31.61 release documents and direct recipe JSON.
- Corrected all identified UI, sorting and source-data issues in PF-EMI's code; manual runtime testing remains unchecked.
- Native meat cooking remains gameplay-valid upstream. PF-EMI's hide filter is viewer-only.
- Confirm current validation results using `python tools/verify_pf_emi_pf19.py`.

### pf.19 local verification

- `python tools/verify_pf_emi_pf19.py`: 39 of 39 local source-contract checks passed.
- JDK 21 JavacTask parsed all **9** new/modified Java files with zero parser diagnostics; not a mod dependency compile.
- Attempted `:xplat:compileJava --offline --no-daemon` after using an LF-normalized temporary shell wrapper. It failed before project configuration with `java.net.UnknownHostException: services.gradle.org` while fetching Gradle 9.2.1.
- No in-game UI, recipe transfer, recipe tree, or singleplayer/multiplayer verification is claimed.
