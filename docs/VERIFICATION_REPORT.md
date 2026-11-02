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
