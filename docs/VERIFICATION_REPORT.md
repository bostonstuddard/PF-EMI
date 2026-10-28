# PF-EMI pf.11 — Verification report

**ProjectFlint reference:** `bostonstuddard/ProjectFlint` `main`, commit [`6f45b748eb808e50f9945ee174f9ccf255362862`](https://github.com/bostonstuddard/ProjectFlint/commit/6f45b748eb808e50f9945ee174f9ccf255362862), `mod_version=0.31.48-b`, `minecraft_version=1.21.11` as read on October 28, 2026. **PF-EMI archive input:** supplied `1.1.23-pf.10` Minecraft 1.21.11 source ZIP. **Output:** `1.1.23-pf.11` source patch, *not a tested JAR*.

## Performed in this pass

- Confirmed upstream ProjectFlint version and recent commit, and read authoritative `ModItems.java`, `ModBlocks.java`, `WoodenGrinderRecipeRegistry.java`, `PrimitiveStoveRecipeRegistry.java`, `BookshelfRecipeRegistry.java`, `FireclayForgeMultiblock.java`, `FireclayForgeBlockEntity.java`, `BasinBlockEntity.java` and ProjectFlint's current changelog through the GitHub connection. Inspected related symbol references for sleeping bag, strawberry, hardwood and mining.
- Confirmed uploaded PF-EMI included a Sleeping Bag placement route using 8 Woven Wool; it was not actually absent in the given pf.10 sources. The 'bed' case should be tested for source parity and whether the recipe can be found in EMI's interface.
- Confirmed PF-EMI already contains categories for block recovery and mob loot (but they are manually enumerated and incomplete).
- Added code for Fireclay Forge structure / materials, selected late Wood Grinder and Oven recipes, research, Basin, Hardwood, Strawberry, related wood fuel and block/animal drops. The source definitions directly checked are marked `[x]` in the master checklist; everything else remains unchecked.
- Modified the viewer canvas, scale floor and title label separation. This is a source/layout change only, not a screen-verified visual fix.
- Rewrote `build.bat` to directly call Fabric builds; removed the absent `neoforge` Gradle subproject from `settings.gradle` and disabled it in `gradle.properties`. The original source has a `forge` folder but Forge is not included in the build or asserted compatible.
- Consolidated historic changelog files into a concise single changelog; created per-item checklist and local audit script.
- Static inspection: modified Java files have balanced delimiters after string/comment stripping; required registration calls and Forge material markers are present; Python audit utility syntax-checks.

## Unverified and known incomplete integration

- **No Gradle build:** the wrapper attempts `https://services.gradle.org/distributions/gradle-9.2.1-bin.zip` but the container cannot resolve `services.gradle.org` (`UnknownHostException`). Java 21 is available, but not the required Gradle 9.2.1 installation. Do not infer compiled correctness from delimiter checks.
- **No runtime/GUI verification:** no Minecraft client in this sandbox. Tooltip positions, 3D item/block appearance, recipe registration, recipes that return vessels, runtime errors, multiplayer and tooltip visibility remain unchecked.
- **No complete byte-for-byte ProjectFlint checkout:** selected GitHub code files were read through the connector. The complete current Java source tree, resources, loot-table JSON, data tags and mixins were not locally downloaded and exhaustively diffed; **100% parity is not certified**.
- **Component-specific recipes:** EMI's generic parchment ingredient/result does not preserve `RecipeParchmentItem.boundStack(...)` metadata for the specific research. Unbound and staged research are separate server rules.
- **Tags and permissions:** a representative set of stripped logs, planks, raw meats and tools is shown but not a dynamic exact read of every accepted tag and condition. Several cookbook pages summarize rather than execute source logic.
- **Loot:** block and entity loot lists are curated. Dynamic vanilla loot tables, conditional drop counts, probabilities, tool gating, Fortune/Looting, age, support-loss, player-placed provenance and on-fire replacements are not all proven. One turtle-meat page deliberately does not assert a verified count.
- **Hardwood:** source-claimed personal and 3×3 worktable recipes are not all catalogued by this pass. The newer native crafting recipes should be verified on a real ProjectFlint instance before claiming full coverage.
- **Forge/NeoForge:** not built or verified; this source distribution only targets the Fabric build path.

## Next independent verification pass

1. Obtain an exact fresh ProjectFlint checkout and run `python tools/audit_projectflint_emi.py path/to/ProjectFlint` from this PF-EMI root; attach output to the checklist.
2. Enumerate every registered recipe from gameplay registries and generated/datapack recipes, compare to all registered EMI recipes **by output, count, inputs, tags, catalysts, consumption, timing and conditions**. Fix all mismatches.
3. Run `build.bat` with JDK 21 and a working Gradle internet/cache, then load PF-EMI with that exact ProjectFlint build and gather client/server logs.
4. Inspect multiblock Well and Forge (full/cutaway, every rotation, GUI scales 1–4), plus controller interaction, material hover and zoom/fitting.
5. Inspect block/animal loot through both EMI recipe lookup routes and actual in-world test with every applicable tool/condition.
6. Mark individual checklist lines `[x]` only after their source and runtime facts are directly verified. Recheck upstream GitHub HEAD before release.
