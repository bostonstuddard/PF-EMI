# PF-EMI 1.1 — Development changelog

**Minecraft:** 26.3  
**Loaders:** Fabric, NeoForge, Forge  
**Branch status:** development; Fabric and Forge singleplayer logs show full discovery counts after the render-thread fix. NeoForge verification is outstanding (the latest supplied log has no EMI mod loaded).  
**Last updated:** 2026-10-28 (Dev 3.13 enchantment information text contrast)  
**Base:** the uploaded 26.3 source, plus all v30 changes. This file tracks everything retained for the eventual 1.1 release.

This changelog is written for developers and pack maintainers. Each entry explains the problem, the implementation, and any impact on integrations. Links to upstream issues mean **related work in PF-EMI**, not a claim that those issues have been closed upstream or reproduced under every supported loader.

## Unreleased 1.1 — Dev 3.13 (enchantment incompatibility text contrast)

**Reported:** Enchantment information pages render the list under “Incompatible with:” in Minecraft light gray (`#AAAAAA`) on EMI's light-gray recipe panel (`#C6C6C6`). In the provided Sharpness screenshot, the exclusion names are barely distinguishable from the panel background. This is a local PF-EMI accessibility defect; no upstream EMI issue number has been confirmed.

### Implementation

- **`EmiVanillaDiscovery.registerEnchantments`** changes the styling of the names generated from `Enchantment.exclusiveSet()` from `ChatFormatting.GRAY` to `ChatFormatting.DARK_GRAY` (`#555555`). The list remains visually secondary, but the significantly darker type is readable against the standard light EMI panel.
- The change affects **only** the text styling of the incompatibility list. It does not change the registry-sourced exclusions, accepted-item groups, enchanted-book output levels, enchantment category, recipe filtering, or any other style in EMI.
- **Loader parity:** This is shared `xplat` code and applies to Fabric, NeoForge and Forge when the mod is installed. No loader-specific implementation is required.

### API / ABI and testing

- **API/ABI:** None. The change is to an internal `Component` formatting constant; no public signatures, serializers, config values, network packets, or dependencies change.
- **Validation:** Source-level replacement count and cumulative patch-content comparison passed. The 1.1 Dev 3.12 patch files are otherwise byte-identical. No Java 25 compilation or in-game contrast retest was performed.
- **Recent log context (separate from this fix):** Fabric and Forge singleplayer logs from 2026-10-28 show 40/40 status effects, 43/43 enchantments, 13 professions and 397 trades registered and visible. The supplied NeoForge log lists only `minecraft` and `neoforge` and warns `emi ... -> MISSING`; it does **not** provide a meaningful PF-EMI regression test. The isolated Fabric `DataFixerUpper/ERROR: Not a list: 3837` remains an independent diagnostic, not caused or addressed by this text change.

---

## Unreleased 1.1 — Dev 3.12 (enchantment supported-item grouping)

**User report:** The Enchantments page works on Fabric, but enchantments such as Aqua Affinity, Bane of Arthropods and Curse of Binding show numerous separate armor/tool material variants in the "Accepts" area. Helmets, boots, swords and axes take up most of the page instead of appearing as useful rotating families. No upstream issue number has been established for this PF-EMI-specific display problem.

### Implementation

- **`EmiVanillaDiscovery.registerEnchantments`** now groups *display slots* by equipment type, not by material or the broad `enchantable/armor` and `enchantable/weapon` tags. It uses Minecraft 26.3 item-type tags (`head_armor`, `chest_armor`, `leg_armor`, `foot_armor`, `swords`, `axes`, `spears`, `pickaxes`, `shovels`, `hoes`, `skulls`) for both vanilla and modded items that use those tags.
- An ID-suffix fallback (`*_helmet`, `*_chestplate`, `*_leggings`, `*_boots`, and corresponding tools) covers compatible modded equipment without those tags. Tagged and fallback variants resolve to the same family so there are not two separate helmet groups. A carved pumpkin remains its own special headwear entry; unknown items are never guessed into a group.
- Each family uses **`EmiIngredient.of(List<EmiIngredient>)`**, allowing EMI's own tag optimization or rotating-alternative ingredient behavior. Singletons remain ordinary item slots. The enchanted book remains one slot rotating through all supported book levels.
- **Compatibility remains sourced exclusively from `Enchantment.getSupportedItems()`**. Hiding/disabled-item filtering is preserved. The ungrouped `compatible` list is still used for recipe-input lookups; no new item is added, removed, or represented as enchantable by grouping. Enchantment book outputs, categorization, and registry logic are untouched.

**Expected UI:** Aqua Affinity shows the book plus one helmet-family slot; Bane of Arthropods shows a book and separate sword/axe/spear groups when those types are supported; Curse of Binding shows helmet, chestplate, leggings and boots as separate groups, with carved pumpkins and other special headwear remaining distinct.

### API / ABI and validation

- **API/ABI:** No new or changed public EMI API declarations, recipe signatures, serialized types, config options, packets or dependencies. The changed helper is private, and this is a presentation-only change to existing enchantment info recipes. Existing `EmiIngredient` implementations are reused.
- **Tracker:** Internal PF-EMI UX report based on screenshots from 2026-10-28; not asserted to close an upstream EMI issue.
- **Validation:** Static review against the provided Minecraft 26.3 `ItemTags` source and local grouping invariants; cumulative patch retention and archive integrity checked. Java 25 compilation and in-game rendering have **not** been performed. Test on Fabric first, then NeoForge/Forge; verify all previously supported items still appear through the rotating groups, and modded/hidden items do not leak into an unrelated family.

---

## Unreleased 1.1 — Dev 3.11 (Forge platform classes missing from release JAR)

**Reported:** After applying Dev 3.10, both the root Fabric/NeoForge Gradle build and the standalone Forge Gradle build returned `BUILD SUCCESSFUL`. The post-build validator correctly rejected the Forge JAR because `dev/emi/emi/platform/forge/EmiForge.class` and every other Forge platform class were absent. This is a **broken Forge artifact**, not a compilation failure in the shared source and not the informational Mavenizer `Cache miss!` message.

**Root-cause investigation:** Compared `forge/build.gradle` against the uploaded 26.3 archive containing a previously built, valid Forge JAR. That working configuration used Gradle's conventional `src/main/java` source root and added the shared tree with `sourceSets.main.java { srcDir '../xplat/src/main/java' }`. Dev 3.9 replaced this with an explicit `setSrcDirs(...)`; despite its source-set preflight passing, Forge produced a shared-code-only JAR. The **source-set replacement is the leading regression suspect**, but the exact interaction with ForgeGradle's compile task is not confirmed without executing Gradle under Java 25.

### Implementation

- **Restored the known-working Gradle source-set form** in `forge/build.gradle`. The default Forge `src/main/java` source root is no longer replaced, and shared `xplat/src/main/java` is added non-destructively.
- **Made Forge-only compilation inputs explicit** using a `fileTree` of `src/main/java/dev/emi/emi/platform/forge` and `src/main/java/dev/emi/emi/api/forge` appended to the normal `compileJava` task. The task now checks its actual compiler inputs for `EmiForge.java`, rather than only checking `sourceSets.main.java` membership.
- **Fail at the compilation stage if Forge classes are missing**: immediately after `compileJava`, check for `EmiForge`, `EmiClientForge`, `EmiAgnosForge`, `EmiPacketHandler`, `EmiRecipeSync`, and `ForgeEmiStack` in that task's output directory. Missing classes stop the build *before* a seemingly successful `jar` task.
- **Keep strict post-package verification** in `scripts/verify-forge-jar.ps1`, now checking the same six required classes, plus the existing manifest, mods metadata and Forge Mixin checks. The validator has **not** been weakened or bypassed.
- **No release-name change**: Windows release outputs remain `1.1+26.3+fabric.jar`, `1.1+26.3+neoforge.jar` and `1.1+26.3+forge.jar`. `build.bat --forge-only` continues to avoid rebuilding the already successful Fabric and NeoForge targets; the terminal still waits for a keypress and never opens Explorer.

### Developer API / ABI and validation

**API / ABI:** No changes to `dev.emi.emi.api.*`, JAR public class signatures, metadata coordinates, recipe/category registrations, serialization, networking, Mixin targets, dependencies, or game logic. This is a ForgeGradle build-input and binary-artifact validation fix. Forge loaders require the existing `@Mod` class to actually be bundled; packaging it is restoration of the expected ABI, not a new API.

**Tracker:** PF-EMI internal build regression; no upstream EMI issue number. Do not treat it as a Forge/Mavenizer cache bug.

**Checked:** Compared source-set configuration and known compiled Forge class entries against the older uploaded working 26.3 JAR; verified the patch contents, required source file list and validator structure. **Not checked:** actual Java 25/ForgeGradle execution or runtime behavior. The next acceptance test is `build.bat --forge-only`, followed by confirmation that the Forge JAR passes its validator and the game loads without entrypoint errors. If it fails again, keep the `compileJava` error and the resulting JAR; they will distinguish compilation inputs from packaging loss.

---

## Unreleased 1.1 — Dev 3.10 (async information-page registration / render-thread safety)

**Reported:** Fabric singleplayer on Dev 3.9, 2026-10-28. Effects, enchantments and profession pages stopped appearing after previously working on Fabric. This is a **confirmed local regression**; it is **not an upstream EMI bug report**, and there is no upstream issue number assigned.

### Reproduction evidence and root cause

The supplied runtime log records `[EMI Discovery] Status effects: 8/40 registered`, `Enchantments: 0/43 registered`, and `Villager professions: 0 registered`; meanwhile `Villager trade templates: 397 registered`. For failed entries, the stack trace consistently runs from `EmiReloadManager$ReloadWorker.run` through `EmiVanillaDiscovery`, `EmiInfoRecipe.<init>` and `Minecraft.font.split` to a `RenderSystem`/font texture-upload exception (`Rendersystem called from wrong thread`, sometimes `Close the existing render pass before performing additional commands`). The problem is **not missing enchantment registries or a recipe filter**: the client enchantment registry reported 43 entries.

The shared `EmiInfoRecipe` constructor eagerly wrapped all information text. On Minecraft 26.3, font splitting may lazily bake glyphs and upload GPU textures. EMI's registration callback runs on a background reload worker, where that GPU work is illegal. Which entries happened to survive depended on glyph caching and timing; broad try/catch loops preserved the reload but left partial/empty categories and very large error logs.

### Implementation

- `xplat/src/main/java/dev/emi/emi/api/recipe/EmiInfoRecipe.java`: **defer** `Font.split` to the first render-thread layout request instead of invoking it in the constructor. Preserve the supplied `List<Component>` as immutable source text; cache the wrapped `FormattedCharSequence` list after render-thread layout. The cached list is published with a private `volatile` field.
- `getDisplayHeight()` uses exact wrapped text when invoked on the render thread. Before layout on a background thread it returns a safe provisional line-count height without touching font glyphs; normal recipe-screen layout obtains the real height. `addWidgets()` uses the same cached text for pagination, preventing the earlier constructor crash across status effects, enchantments, professions **and other info pages**.
- **Do not** schedule blocking render-thread calls from the reload worker: that could deadlock if a render-thread task is waiting for the reload. Do not mute `RenderSystem` checks, suppress error reporting, change plugin registration order, or revert Dev 3.6's searchable effect/enchantment model. Villager-trade logic remains unchanged.
- The fix addresses the common registration failure diagnosed from the log; it does not claim all previous loader-specific problems are fully resolved without another in-game test.

### API / ABI and developer considerations

The public `EmiInfoRecipe(List<EmiIngredient>, List<Component>, Identifier)` constructor and all public methods remain **signature-identical**. The new source-text and cache fields and layout helper are private. Existing `EmiRecipe`, `EmiRegistry`, `EmiPlugin`, serialization contracts, category IDs, networking and loader metadata are unchanged. This is a **behavioral change**: font-dependent text wrapping is deferred until render-thread layout, and worker-thread `getDisplayHeight()` may return a provisional height before first UI layout. Addons should not assume an information recipe constructs glyph textures during registration or query exact text layout off-thread. There are no copied third-party source fragments in this fix.

### Verification needed

Static inspection confirms `EmiInfoRecipe`'s constructor no longer invokes `Font.split` and that all text-layout calls are guarded by render-thread detection. **No Java 25 compilation or runtime testing was performed**. In Fabric singleplayer, verify the log reports **40/40 effects, all 43 synchronized enchantments, and registered professions**, with no `Rendersystem called from wrong thread` during EMI reload; check potion associations, enchantment books at several levels, category visibility and pagination. Then repeat for Forge, NeoForge and multiplayer, including resource reloads and screen transitions. The expected counts are tied to the *specific registries seen in the attached log* and may legitimately change with datapacks or mods.

---

## Unreleased 1.1 — Dev 3.9 (Forge entrypoint packaging and release filenames)

**Reported by build log, 2026-10-28:** `:fabric:build`, `:neoforge:build` and Forge's standalone `:build` all completed, but the Windows Forge post-build validator failed with `Forge JAR is missing EmiForge.class.` The three copied outputs also used the previous `1.0+26.3+<loader>.jar` filename prefix despite the 1.1 development branch. Mavenizer's informational access-transform cache miss is unrelated to the entrypoint error.

### Changes and implementation

- **Forge source inclusion:** `forge/build.gradle` now sets both `src/main/java` (Forge `@Mod` entrypoint, loader lifecycle handlers, packet support, public loader API) **and** `../xplat/src/main/java` (shared EMI code) as explicit `main` Java source roots. This is a defensive correction to the ForgeGradle source-set setup, which previously appended only the shared tree and left inclusion of Forge-local sources implicit. **The actual contents of the failing JAR were not supplied, so this is the identified source-set risk, not a byte-for-byte diagnosis of the produced JAR.**
- **Early validation:** Added `verifyForgeEntrypointSource`, a Gradle prerequisite of `compileJava`, which fails clearly if `EmiForge.java` isn't included in the main source set. The existing `scripts/verify-forge-jar.ps1` remains authoritative after packaging: it still checks the compiled `EmiForge.class`, Forge metadata, manifest, Mixin configuration, and artifact size. If the class is absent, it now lists nearby packaged Forge platform classes to clarify the failure. **The validator is not disabled or made less strict.**
- **Release filenames:** Windows `build.bat` and Linux `build.sh` consistently use `1.1+26.3+fabric.jar`, `1.1+26.3+neoforge.jar`, and `1.1+26.3+forge.jar`. Matching stale `1.0+26.3+<loader>.jar` outputs are removed when rebuilding those loaders, to prevent mistaking previous artifacts for new output.
- **Documentation:** `README.md` now lists the actual 1.1 release filenames; internal `mod_version` properties and file metadata are intentionally unchanged. The source archive remains patch-only.
- **Existing build interaction preserved:** Windows terminal pauses on either outcome, Forge can still be rebuilt with `build.bat --forge-only`, and the script does not open File Explorer automatically.

### Developer API / ABI and compatibility

No Java code, public EMI API/ABI declarations, registry/category identifiers, serializer, Mixin list, wire format, dependency versions, or loader metadata were changed. This is **build/source-set, validation diagnostics, and artifact-naming only**. A correct Forge artifact should contain its existing platform classes and the shared EMI implementation. Release filenames are *external packaging labels*; embedded mod version coordinates remain those configured by Gradle.

**Tracking:** PF-EMI internal build defect (no upstream EMI issue number); not considered an upstream EMI bug.

**Verification:** Static Gradle/BAT/shell source checks and patch-integrity comparison only; no Java 25 toolchain is installed in this environment. User's Dev 3.8 log proves successful compilation of Fabric/NeoForge and of Forge's prior source set, **not** successful Forge JAR validation. Check the resulting Forge JAR for `dev/emi/emi/platform/forge/EmiForge.class` and test startup across all three loaders after rebuilding Dev 3.9.

---

## Unreleased 1.1 — Dev 3.8 (Forge/NeoForge discovery resilience and config text)

**Reported in singleplayer:** Forge and NeoForge both show villager trades, but neither shows the expected enchantment pages. Forge has no visible status-effect pages, while NeoForge displays only a partial set. A config screenshot also shows the literal translation keys for the new EMI button position/offset and inventory highlighting settings. Fabric's categories are reported working. There are **no new Forge/NeoForge logs attached**, so these are source-level corrections and diagnostics, not a confirmed cross-loader resolution.

### Implementation

- **Status effects:** Moved the `Potion.getEffects()` guard *inside* each potion's exception boundary. Before this change, an exception while reading any potion's effect list could abort the entire status-effect registration pass before a single independent effect entry was added. A bad potion now only removes its own associations, while the full built-in effect registry can still be visited. Per-effect handling also reports linkage failures rather than silently causing the remaining entries to disappear. The independent effect identity introduced in Dev 3.6 is unchanged.
- **Enchantments — registry selection:** Prefer the synchronized registry of the **current client connection** when available, then consider the client world's registry, and only in integrated singleplayer consider the server registry when it contains more entries. This avoids unconditionally using server-side holders to construct client-facing `ItemStack` components and logs which registry was used and how many enchantment entries were found. The server fallback remains singleplayer-only; no synchronization packets were introduced.
- **Enchantments — supported items:** Replace the `all registered items × each enchantment` scan with the `Enchantment.getSupportedItems()` holder set. This is both more faithful to datapack-defined compatibility and avoids treating an unrelated item's item-stack lookup failure as grounds to omit an entire enchantment page. Each supported item is isolated; unresolved holder sets still permit the book/info page to register. Exact enchanted-book variants continue to use the existing EMI component-aware stack indexing.
- **Enchantments — per-level resilience:** Variant creation and EMI stack/alias registration are handled independently, so a single invalid level or alias no longer discards an otherwise valid enchantment. If no variants at all can be built, the missing page is explicitly logged. The existing public `EmiRegistry` methods are used; no parallel index is added.
- **Diagnostics:** `EmiRecipes.bake()` now warns if an information category contains **zero recipes after filters**. Prior registered-vs-visible counts remain intact, so a new runtime log can distinguish registry lookup failures, individual data errors, and data filtered out of EMI.
- **Config labels:** Added the missing English `config.emi.ui.emi-buttons-anchor`, `config.emi.ui.emi-buttons-offset`, and `config.emi.ui.inventory-search-highlight` translations. Enum options were already translated. Other locales may still show untranslated text until those locale files receive translations; no settings or defaults were changed.

### Source and developer compatibility

Files: `xplat/src/main/java/dev/emi/emi/recipe/EmiVanillaDiscovery.java`, `xplat/src/main/java/dev/emi/emi/registry/EmiRecipes.java`, `xplat/src/main/resources/assets/emi/lang/en_us.json`, and this changelog. The villager trading, profession, brewing and Fabric entrypoints have **not** been changed.

**API / ABI:** No edits to `dev.emi.emi.api.*`, API method descriptors, recipe or serializer IDs, wire protocols, dependencies, or config keys. This is an internal registration/lookup behavior change and an additive localization change. Addon-facing semantics remain intended to be compatible; Java 25 compilation and actual binary-compatibility tests are **not yet performed**.

**Verification:** Static source assertions, locale JSON parsing and cumulative archive comparison only. Test Fabric as a regression baseline, then NeoForge and Forge in fresh singleplayer worlds. Collect `[EMI Discovery]` lines and any `Failed to register effect page`, `Failed to create enchantment book variant`, `Failed to register enchantment page`, or `no visible pages after filtering` messages if either category remains missing. No upstream EMI issue number is assigned to these local reports.

---

## Unreleased 1.1 — Dev 3.7 (Forge/NeoForge discovery parity)

**Reported:** Dev 3.6 looks operational in Fabric singleplayer: villager trades, professions, enchantments and status-effect entries render. On NeoForge and Forge, much of the new 1.1 information is not visible. The user's skin also failed to render in one NeoForge singleplayer session. No new Forge/NeoForge runtime logs were provided for this report; the exact reason for each missing category remains unverified.

### Implementation and rationale

- **Decoupled discovery from vanilla recipe translation.** Added `EmiDiscoveryPlugin`, a separate *internal* `EmiPlugin` instance scheduled by `EmiReloadManager` on all three loaders, immediately after the built-in vanilla plugin and before third-party plugins. `VanillaPlugin.register()` no longer calls `EmiVanillaDiscovery.registerCategories()` or its four register methods. If a loader-specific vanilla crafting recipe throws during the main plugin registration, the independent discovery plugin can still register effects, enchantments, professions and trades. It is not registered through both annotation scanning and `fabric.mod.json`; there is exactly one explicit instance per EMI reload.
- **Effect serializer ownership moved with discovery.** `MobEffectEmiStackSerializer` is now initialized in `EmiDiscoveryPlugin.initialize()` via the unchanged `EmiInitRegistry` method. `VanillaPlugin.initialize()` no longer registers it; duplicate registration is avoided, and the effect stack does not depend on the vanilla plugin's loader-specific entrypoint being detected.
- **Built-in vanilla entrypoint recovery.** In `EmiReloadManager`, after collecting loader-discovered plugins, add one `VanillaPlugin` *only if none was found*. This protects against mod-file annotation scanning differences on Forge/NeoForge. An already discovered vanilla plugin is not duplicated; all other mod plugins and their normal discovery remain unchanged. The fallback logs a warning when used.
- **Forge 26.3 trade accessor:** Added only `accessor.VillagerTradeAccessor` to `forge/src/main/resources/emi-forge26.mixins.json`. Forge's production JAR explicitly loads that config, *not* the shared `emi.mixins.json`, so the 26.3 trade bridge was missing the required accessor Mixin. Do not globally enable the shared Mixin configuration on Forge; it contains other injections intentionally excluded in this port.
- **Per-section diagnostics:** Independent discovery logs a start message and preserves existing registry counts and post-filter counts. Errors in a specific information section do not stop other sections, and the exception is still reported with a stack trace. The entrypoint fallback is explicitly logged rather than silently changing plugin behavior.

### Source files

- `xplat/src/main/java/dev/emi/emi/recipe/EmiDiscoveryPlugin.java` (new internal plugin)
- `xplat/src/main/java/dev/emi/emi/runtime/EmiReloadManager.java` (one-time explicit registration and missing-entrypoint fallback)
- `xplat/src/main/java/dev/emi/emi/VanillaPlugin.java` (remove discovery and serializer registrations, leave all vanilla gameplay/viewer recipes intact)
- `forge/src/main/resources/emi-forge26.mixins.json` (single additional 26.3 accessor)
- `CHANGELOG-1.1.md`

**Public API / ABI:** No `dev.emi.emi.api.*` signature was changed. No new external dependencies, packets, recipes, configuration fields, category IDs, serializer type IDs, or user-visible feature semantics. A new *internal* `EmiPlugin` class and one Forge Mixin are added; internal plugin initialization/registration order is observable to addons that introspect recipe registration. `emi:status_effect` serialization remains identical. Already compiled addons must still be tested on all loaders before claiming ABI validation.

**Skin report:** PF-EMI contains no direct player-skin download, player-renderer or skin texture modifications in its own source. No speculative player rendering patch was made. Reproduce with and without PF-EMI using the same NeoForge 26.3 profile and check authlib, skin-service and resource errors in the log; distinguish an invisible player from a default Steve/Alex skin. This is tracked as an *unconfirmed* separate bug, not silently marked fixed.

**Verification:** Static file and class checks only. Fabric singleplayer success is user-reported for Dev 3.6, **not** for the new Dev 3.7 patch. No Java 25 compilation, NeoForge/Forge runtime test, or full ABI integration test performed. In each loader, compare `[EMI Discovery]` counts plus `EmiRecipes.bake()` post-filter counts and verify `emi:status_effects`, `emi:enchantments`, `emi:villager_professions`, and `emi:villager_trading`. If category counts are positive but pages are absent, next inspect recipe filtering and layout rather than modifying registries again.

---

## Unreleased 1.1 — Dev 3.6 (effect identity and enchantment variants)

**Bug report:** Fabric singleplayer, after Dev 3.5: an incomplete-feeling status-effect browser and several missing unique enchantments. This follows multiple ineffective display/index fixes. **Status:** implementation revised; actual in-game completeness has **not** been confirmed. No upstream issue number is assigned to this local bug.

### Investigation and fix

- **Effect identity was represented by potions:** The previous `DiscoveryInfoRecipe` returned its display items as both inputs and outputs. Non-brewable effects therefore had *no independently searchable output*; effects sharing potions became difficult to distinguish in EMI's index. `MobEffectEmiStack` is now an internal phantom stack with a unique `MobEffect` key and the built-in registry identifier, displayed through EMI's standard stack/rendering path. `registerEffects()` calls `EmiRegistry.addEmiStack()` for every effect, regardless of whether a potion exists. Each `EffectInfoRecipe` exposes *only that effect* as its output, and the real component-preserving potion families as **catalysts** (lookup sources, not imaginary crafting ingredients). It continues to index every `POTION_CONTENTS` variant actually registered. No invented obtainable potions, gameplay changes, or extra potion recipes.
- **Effect favorites/pack data:** New internal `MobEffectEmiStackSerializer`, registered in `VanillaPlugin.initialize()` via **`EmiInitRegistry.addIngredientSerializer`**, uses JSON type `emi:status_effect` and the effect's namespaced ID. Its `id`, `amount` and `chance` round-trip via existing EMI serializer infrastructure. Missing IDs return `EmiStack.EMPTY` (as with EMI's other serializers). Effect stacks show a default potion silhouette plus a small marker using the effect's registered color until a proper registered-effect sprite renderer can be developed; the tooltip and search index use the real, localized effect name. This visual fallback is **not** a claim of full EMIffect icon parity.
- **Enchantment book variants:** Each enchantment info entry now indexes an exact **single-enchantment book at every supported level**, not just the maximum-level result. Distinct levels preserve strict `STORED_ENCHANTMENTS` component comparison. Missing variants are registered through `EmiRegistry.addEmiStack()` without replacing existing creative-tab entries, and each gets the localized enchantment name as an EMI search alias. The page's outputs are only its books; applicable tools remain lookup inputs and not supposed outputs. No change to anvil/vanilla enchantment generation.
- **Authoritative singleplayer registry:** Registry lookup now prefers the integrated server's complete datapack enchantment registry when singleplayer is active, instead of using it **only** when the client registry is empty. The synchronized client registry is still used when there is no integrated server. No remote-server assumptions or new packets have been introduced.
- **Diagnostics:** Keep existing per-entry exceptions, total registration and post-filter counts. New index entries can be inspected from EMI's normal sidebar/search. A world having four pages does not by itself establish that it is missing effect definitions; the vanilla 26.3 bootstrap defines 40. More actual potion forms can exist than distinct effect types; both types of information are now represented separately.

### Files and compatibility

- `xplat/src/main/java/dev/emi/emi/recipe/EmiVanillaDiscovery.java` — new effect/variant registration, two internal recipe overrides, authoritative registry source.
- `xplat/src/main/java/dev/emi/emi/stack/MobEffectEmiStack.java` — internal effect-specific EMI stack (new).
- `xplat/src/main/java/dev/emi/emi/stack/serializer/MobEffectEmiStackSerializer.java` — internal serializer (new).
- `xplat/src/main/java/dev/emi/emi/VanillaPlugin.java` — serializer registration through existing `EmiInitRegistry` API.
- `xplat/src/main/resources/assets/emi/lang/en_us.json` — additive tooltip translation.
- `CHANGELOG-1.1.md` — this entry.

**Public API / ABI:** No modifications to `dev.emi.emi.api.*`, no altered methods or descriptors on public interfaces/classes, and no new public method signature. The **new ingredient serialization type** (`emi:status_effect`), added effect sidebar entries and recipe output/catalyst semantics are intentional **behavioral/serialized-data additions**: any add-on parsing saved stack JSON or scraping category recipes should recognize or safely ignore this new type. Existing `EmiRecipe` and `EmiRegistry` implementations are reused. No changed config, packet schema, required dependency, registry key or loader entrypoint. Source-level ABI invariants checked; **binary compatibility not verified** until an actual Java 25 build and compiled-addon tests.

**Attribution:** Design reviewed against [EMI Enchanting](https://github.com/fzzyhmstrs/EMI_enchanting), which indexes per-level books, and the [EMIffect feature description](https://www.curseforge.com/minecraft/mc-mods/emiffect-status-effects-emi-plugin), which exposes searchable phantom effect entries. No third-party implementation, sprites, or translation database was copied. Original EMI contribution guidance on minimal diffs, preserving API/ABI and reusing EMI abstractions guided this implementation. The working trades implementation has not been changed.

**Required acceptance tests before calling this fixed:** Fabric singleplayer: (1) search non-brewable `Haste`, `Darkness` and `Hero of the Village` in sidebar and open each effect page; (2) check Night Vision and Poison potions against their actual drinkable/splash/lingering/arrow forms and ensure unrelated effects are not linked; (3) verify standard, long and strong potion variants, and potion mixtures if present, do not collapse to a generic tag; (4) search `Mending`, `Soul Speed`, `Swift Sneak`, `Wind Burst`, and a common multi-level enchantment; (5) inspect books of different levels in sidebar/recipes; (6) compare `[EMI Discovery]` registered vs post-filter totals, and ensure villager trading still works. Then repeat equivalent tests on NeoForge and Forge plus addon ABI tests. This patch has **not been compiled or run** here.

---

## Unreleased 1.1 — Dev 3.5 (effect coverage and enchantment discovery)

**User report:** On Fabric singleplayer, Dev 3.4 showed only a few pages of effects, linked some effect pages to incorrect potions/arrows, and offered no Enchantments category. Villager trades work and remain unchanged. No upstream issue number is claimed for this local report.

### Fixes

- **Potion/effect identity — `EmiVanillaDiscovery.registerEffects()`:** Replaced generic `EmiIngredient.of(variants)` with EMI's explicit `ListEmiIngredient`. The generic constructor can compact a collection of item types into a matching tag; an item tag does **not** retain `POTION_CONTENTS`, so the grouped slots and recipe lookup can lose which registered potion they represent. Each drinkable, splash, lingering and tipped-arrow item now retains the exact `Holder<Potion>` used when building its `POTION_CONTENTS`. The mapping continues to be based on the actual `Potion.getEffects()` entries, never guessed from item names or effect descriptions. This applies to registered modded potions too. No synthetic potion outputs were introduced.
- **Complete effect coverage and diagnostics:** Keep one category entry for **every** entry in `BuiltInRegistries.MOB_EFFECT`, including effects with no associated potion (such as Haste and Darkness). Minecraft 26.3's bundled `MobEffects` bootstrap defines **40 vanilla effects**, so four ten-entry pages could be correct; the new logs explicitly show registered/expected counts and how many effects have real potions. The recipe filters now report how many pages survive baking. This helps distinguish missing effect pages from effects that simply have no potion variant. First-class searchable effect icons remain a separate integration project.
- **Enchantment discovery — `EmiVanillaDiscovery.registerEnchantments()`:** Read the synchronized 26.3 data-pack enchantment registry, with a singleplayer integrated-server fallback when the client view is empty. If no authoritative data is available, the Enchantments category now has an explicit explanatory page rather than vanishing. Per-enchantment failures are logged with IDs and registered/expected totals.
- **Enchantment indexing — `EnchantmentInfoRecipe`:** Corrected the semantics of info pages. Previously, an information page claimed that **every compatible item** was an output, potentially subjecting the whole page to recipe-disabling filters and producing incorrect recipe sources. Compatible items remain visible in EMI's stock info widgets and are grouped into one alternative *lookup* input (allowing players to discover applicable enchantments from a tool without treating it as a consumed crafting resource). **Only the actual enchanted book** is indexed as an output, using strict component comparison so distinct enchantment books do not become one undifferentiated item. This remains informational and cannot be crafted through recipe filling or recipe trees.
- **Post-filter checks — `EmiRecipes.bake()`:** Add `[EMI Discovery] ... visible pages after recipe filters` lines for effects, enchantments, professions and trades. Registration totals can differ from visible category totals when another mod or modpack disables stacks/recipes; this is now observable without a debugger.

### API / ABI and developer notes

- No additions or removals of public `dev.emi.emi.api` types, methods or binary descriptors. `EmiRegistry`, `EmiRecipe`, plugin entry points and the existing optional `EmiPartialConsumptionRecipe` contract remain unchanged. No network, recipe codec, config, or resource-serialization changes.
- `ListEmiIngredient` and the internal `EnchantmentInfoRecipe` subclass reuse EMI's existing rendering and ingredient/index contracts. The synthetic recipe IDs and category IDs remain unchanged. **Behavioral change:** enchantment information no longer advertises unenchanted compatible tools as recipe outputs; integrations relying on that erroneous behavior should use the intended enchantment book or the category instead. Potion links now preserve exact data components.
- All changes stay in the 26.3 shared source, plus one English localization key. Existing villager trading logic and mixins remain byte-for-byte unchanged. No external mod code was copied.
- **Verification pending:** Compile with Java 25; confirm effect registration vs post-filter totals and 40 vanilla effect IDs on Fabric; check representative potions/tipped arrows and non-potion effects; verify a complete enchantment list with compatible tools and book links; repeat on Forge/NeoForge and regression-test trades. Not marked fully fixed until runtime verification.

## Unreleased 1.1 — Dev 3.4 (status-effect page usability)

**Reported after Dev 3.3 Fabric testing:** Villager trades were reported to work, but the status-effect browser still felt unfinished. The report did not include an example screenshot or log, so this patch addresses *source-confirmed display defects*, not an assumed visual symptom. No other discovery category was redesigned.

**Problem and implementation:**

- `EmiVanillaDiscovery.registerEffects()` previously placed **four separate EMI slots per potion** (drinkable, splash, lingering and tipped arrow), then repeated them for each matching registered potion. That could crowd an effect page and bury its information. Each registered potion is now a single rotating `EmiIngredient` containing its existing available item variants. EMI still indexes every individual `EmiStack` inside the grouped ingredient for ordinary recipe lookup. The code de-duplicates repeated appearances of the same effect within a single potion and respects the existing hidden-item filters.
- Vanilla effect descriptions were generally missing. The previous code looked only for optional localization keys provided by another addon or pack, so the page often contained just the effect name and `Beneficial/Harmful/Neutral`. Added **39 independently authored, short English vanilla status-effect summaries** under `emi.discovery.effect.description.minecraft.*` as a fallback. It still checks `effect.<namespace>.<path>.description` and `.desc` first so existing addon/modpack localizations can override the fallback. Modded effects with no description remain supported and do not get invented text.
- Pages now state the **number of indexed registered potion variants** (one slot per potion ID, with its item forms grouped); no-potion effects keep their explicit explanation. We do not fabricate an unobtainable potion for effects with no recipe.
- `EmiInfoRecipe.getDisplayHeight()` and `addWidgets()` previously reserved an empty item row even for an info page with zero stacks. Corrected that calculation for all empty info pages so text starts without a pointless gap. The status-effect pages alone now cap their display height at 144 px; EMI's existing `PageManager` and scroll buttons handle longer text. No new custom renderer or duplicate pager was introduced.

**Changed files:** `EmiVanillaDiscovery.java`, `EmiInfoRecipe.java`, `assets/emi/lang/en_us.json`, and this changelog. There is **no change to the working villager trade code**, profession registry handling, fuel logic, config, serialization, networking, or build scripts.

**API/ABI:** No new or removed public Java methods, constructors, fields, descriptors, recipe category identifiers, or packets. `EmiInfoRecipe` retains its public signatures; its **empty-stack layout height changes behaviorally**, which plugins relying on that accidental gap might observe. Grouped potion ingredients still implement EMI's existing public `EmiIngredient` contract. Localization keys are additive, and translation precedence is documented above. No third-party code or JEED text copied.

**Tracker:** PF-EMI internal report, no upstream EMI issue number claimed. Related feature inspiration: [EMIffect](https://modrinth.com/mod/emiffect), MIT; no EMIffect source was copied. This does **not** yet add searchable standalone status-effect `EmiStack` entries or icon sprites, nor does it promise full EMIffect feature parity.

**Verification:** JSON syntax, patch file layout, and source-level invariants only. Not built or run with Java 25. Requires live Fabric verification of (a) Night Vision plus a long/strong potion effect, (b) a non-potion effect such as Darkness, (c) long descriptions and paging, (d) alternate potion-form slots, and (e) dedicated-server data. Forge and NeoForge remain untested for this change. If the reported wonkiness refers to broken icons, incorrect effect names, missing recipe access, or crashes, an in-game screenshot and log are still needed to isolate those separate failures.

---

## Unreleased 1.1 — Dev 3.3 (discovery categories and visibility correction)

**User report (Fabric):** Dev 3.2 built and launched, and fuel browsing worked. The only visible new information tab was `Info`, apparently containing the Night Vision entry; enchanting, professions, trading, and the other effect pages were not apparent. **This invalidates the earlier assumption that the Dev 3 feature integrations were useful in-game.** Compilation and a working fuel tab are not acceptance tests for those categories.

**Root issue identified by source audit:** Dev 3 put effect, enchantment, and profession data into EMI's general `EmiInfoRecipe`, whose `getCategory()` always returns `emi:info`. This was not a standalone browser like the referenced addons. In addition, `registerTrades()` returned *before registering the trade category* whenever the client's `TRADE_SET` registry was absent. Registration steps were wrapped in an exception handler which could abort an entire discovery pass after one bad entry, without exposing successful-registration counts. The single Night Vision page is a useful symptom, but it does **not by itself prove** which additional recipes were registered or filtered; a new runtime log is required to confirm the full cause.

**Source changes:**

- `EmiVanillaDiscovery.registerCategories()` now registers `emi:status_effects`, `emi:enchantments`, `emi:villager_professions` and `emi:villager_trading` **unconditionally** through `EmiRegistry`, including appropriate workstation anchors (potions, enchanted books/enchanting tables, villager eggs/job sites, emeralds). `VanillaPlugin.register()` invokes this alongside the original EMI categories. No custom UI or second indexing system was introduced.
- Use a private `DiscoveryInfoRecipe extends EmiInfoRecipe` that overrides only `getCategory()`. It reuses EMI's normal information widgets, category tabs and manager; the public `EmiInfoRecipe` constructors and descriptors are untouched.
- Enumerate **all** registered `MOB_EFFECT` entries rather than just effects reachable from potions. Effect pages without potion variants remain browsable through the Status Effects category (and show `No registered potion for this effect`), but are not misleadingly linked to unrelated items. Existing optional description localization keys and potion variants are preserved.
- For professions, enumerate the built-in `POINT_OF_INTEREST_TYPE` registry, which Minecraft 26.3 exposes without a nullable dynamic-world POI lookup. Preserve job-site predicate matching and modded registered POI types.
- For trades, look for the active client trade-set registry; in singleplayer only, also check the integrated server's registry if absent on the client. For dedicated multiplayer clients without the authoritative registry, show an explicit unavailable-data page **instead of silently omitting the category or inventing trades**. Prices and outputs remain *potential* template values, not guaranteed offers. No server packets added.
- A failure for one effect, enchantment, profession or trade now records the corresponding registry ID and continues to the next entry. Each discovery section emits an `[EMI Discovery]` registration count to the terminal. This is a diagnostic, **not an assertion that the game displays them successfully**.
- Add localized category labels, the no-potion note and the trade-data-unavailable explanation. No third-party description text or addon source was copied.

**How to browse in the next Fabric test:** From the EMI recipe viewer, use `View Recipes` on a potion, enchanted book or enchanting table, villager spawn egg, or emerald to access the corresponding categories. The new category tabs are separate from `Info`. Test both the ingredient-linked view and workstation/category navigation, and compare the `[EMI Discovery]` log counts against the visible pages. On unsupported remote servers, the Villager Trading category deliberately shows a data-unavailable notice.

**Developer API/ABI:** No existing `EmiPlugin`, `EmiRegistry`, `EmiRecipe`, `EmiInfoRecipe`, recipe-handler or serialized ingredient signatures changed; `DiscoveryInfoRecipe` and discovery category constants are internal. Four new synthetic category IDs and their localization keys are **observable data/API surface**, although not new public Java symbols. Existing synthetic recipe IDs remain `emi:/discovery/...`; consumers filtering `emi:info` will no longer see these pages under that category. No configuration, network, gameplay, saved-state or packet changes. Test binary addons separately before the 1.1 release.

**Limitations / acceptance gate:** Java 25 compilation and the four categories' actual presence on Fabric, Forge and NeoForge are not verified here. All-feature parity with EMI Loot, EMI Enchanting, EMI Professions, EMI Trades, EMI Ores, EMIffect or JEED is **not** implemented; Dev 3 was only limited registry-driven information pages. The loot-table/ore analyzers, standalone effect icons, true server-authoritative trade sync and enchantment-table probabilities are still missing. User-provided Dev 3.2 Fabric evidence should take precedence over earlier optimistic descriptions.

**Tracker:** PF-EMI internal defect (no upstream EMI issue number claimed). Previously referenced addons are feature inspiration, not upstream issue closures.

---

## Unreleased 1.1 — Dev 3.2 (recipe-manager compilation hotfix)

**Reported during the next user build attempt on October 27, 2026.** After restoring the NeoForge platform property, Gradle configured both Fabric and NeoForge successfully but failed at `:xplat:compileJava`. The compiler reported `cannot find symbol: variable byWorkstation` at `EmiRecipes.java:115`.

**Root cause:** The asynchronous recipe-reload change in v30 added a workstation index publication immediately after constructing the initial manager. The static `EmiRecipes.manager` field is intentionally typed as the public `EmiRecipeManager` interface, which does not expose `byWorkstation`; that field belongs to the private nested `EmiRecipes.Manager` implementation. The worker's later publication already uses a correctly typed local `Manager` variable.

**Change:** In `EmiRecipes.bake()`, construct `Manager initialManager`, assign it to the existing public-interface-typed `manager` field, and publish `initialManager.byWorkstation`. This retains both synchronous workstation lookup and the protected asynchronous worker publication path. No field was added to the public interface, no casts were introduced, and the initialization logic is otherwise unchanged.

**Developer API/ABI:** No public symbols, signatures, class layouts, plugin callbacks, packet formats, configuration formats, or recipe contracts changed. Only a local variable was introduced to reference the existing private implementation.

**Validation:** Exact source-level type/reference check and cumulative patch ZIP integrity. User's log confirms Gradle configuration succeeded after Dev 3.1; no Java 25 build was performed for this patch. The subsequent compile stages and in-game behavior remain unverified.

---

## Unreleased 1.1 — Dev 3.1 (NeoForge configuration hotfix)

**Reported during the first user build attempt on October 27, 2026.** Gradle stopped while evaluating `neoforge/build.gradle:72`, reporting `Could not find method neoForge()` on `DefaultDependencyHandler`. Java compilation was not reached.

**Root cause:** The cleaned 26.3 baseline and Dev 3 working tree omitted `neoforge/gradle.properties`, which had existed in the previously working v27/v28 sources. Architectury Loom requires the NeoForge subproject to declare `loom.platform=neoforge` so it can register the `neoForge` dependency configuration.

**Change:** Restored `neoforge/gradle.properties` containing `loom.platform=neoforge`. Left `neoForge "net.neoforged:neoforge:${rootProject.neoforge_version}"` in `neoforge/build.gradle` unchanged, per Architectury's supported DSL. No plugin version, dependency version or source behavior changed.

**Developer API/ABI:** No Java API or ABI changes. No additions to network protocols, data formats, saved state, config surface or runtime behavior. This repairs build configuration only.

**Validation:** Compared against the known-working v27/v28 config, checked the restored property, and verified patch/archive integrity. Gradle and Java 25 builds are **not** executable in this environment; the full Fabric/NeoForge/Forge build is still unverified. Do not mark this issue resolved at runtime until the build completes on all loaders.

---

## Unreleased 1.1 — development pass 3 (addon feature integration)

**Scope:** add information players previously needed separate EMI addons to view, without introducing a second item index, modifying gameplay, or assuming data that a client cannot access. This pass is **source-only**; no compilation, launch or multiplayer testing has been performed. Dev 1, Dev 2, and v30 changes remain included in the single cumulative patch.

### Potion effects: registry-derived effect information

**Feature reference:** [EMIffect](https://modrinth.com/mod/emiffect), [JEED](https://www.curseforge.com/minecraft/mc-mods/just-enough-effect-descriptions-jeed). **No direct EMI upstream ticket.**

**Code:** new `recipe/EmiVanillaDiscovery.java`, `VanillaPlugin.register()`, `assets/emi/lang/en_us.json`.

- During EMI's normal recipe-registration pass, enumerate `BuiltInRegistries.POTION` and associate each potion's actual `MobEffectInstance`s with the `MOB_EFFECT` registry entry. Add a synthetic `EmiInfoRecipe` for each effect represented by a registered potion.
- Link normal, splash and lingering potions and tipped arrows as visible EMI ingredients. Existing EMI uses/recipe navigation can discover the effect pages from those stacks. Preserve the modpack's `c:disabled_from_recipe_viewers` exclusions for the item variants.
- Label effects beneficial, harmful or neutral using `MobEffect#getCategory()` and distinguish instantaneous effects. Include the localized effect name.
- Look for **both** `effect.<namespace>.<path>.description` and `.desc` in the loaded language files, in that order. This matches the public extension convention documented by EMIffect / JEED; it does not embed or republish JEED's description database. If neither key exists, omit the description instead of displaying a raw translation key.
- Unique synthetic IDs use `emi:/discovery/effect/<namespace>/<path>`. The source data is the client's currently known potion registry. Effects that **have no registered potion** are not yet standalone index entries; implementing first-class effect icons and a compatible serializer needs additional API design.

**Mod developer usage:** supply a resource-pack or mod language entry such as `"effect.examplemod.snowblindness.description": "Reduces visibility during snowfall."`, and register your normal potion/effect types. No PF-EMI-specific Java dependency or registration hook is required for this feature. English vanilla descriptions are not bundled from JEED.

### Enchantments: compatibility, levels, curses and descriptions

**Feature reference:** [EMI Enchanting](https://modrinth.com/mod/emi-enchanting). **No direct EMI upstream ticket.**

**Code:** `EmiVanillaDiscovery.registerEnchantments()`.

- Read the **active level's enchantment registry**, including datapack/modded enchantments. Show a representative enchanted book at its maximum supported level for each entry, generated using the same `DataComponents.STORED_ENCHANTMENTS` construction already used by EMI's anvil-enchanting recipe.
- List all actual `Enchantment#isSupportedItem()` matches from the item registry (excluding disabled items), maximum level, enchantment weight, curse classification from `EnchantmentTags.CURSE`, and the registry-defined `exclusiveSet` as named incompatible enchantments.
- Render an optional `enchantment.<namespace>.<path>.desc` translation, matching EMI Enchanting's localization convention. No third-party description text was imported.
- Enchantment pages remain informational; **no** enchanting RNG, table odds, anvil costs, item modifications, or invented craft recipes are introduced. If an enchantment is data-driven, the current registry is authoritative.
- Synthetic IDs: `emi:/discovery/enchantment/<namespace>/<path>`.

**Developer note:** item compatibility is obtained from Minecraft's 26.3 enchantment definition, but presented through `EmiInfoRecipe` and `EmiStack`, rather than a hand-drawn inventory GUI. External addons can continue to register real recipe categories through `EmiRegistry`; no existing public API contract is changed.

### Villager professions: workstation discovery

**Feature reference:** [EMI Professions](https://modrinth.com/mod/emi-professions-emip). **No direct EMI upstream ticket.**

**Code:** `EmiVanillaDiscovery.registerProfessions()`.

- Enumerate registered villager professions except `none` and `nitwit`. Resolve their held-job-site predicates against the current level's POI registry, then display the actual block items in an `EmiInfoRecipe` alongside an emerald and villager spawn egg.
- This avoids maintaining a fragile hardcoded table of 26.3 job-site blocks and allows compatible modded professions and POI types to appear automatically where registered.
- Show whether a profession has level-based trade sets. Individual prices are deliberately **not** guessed from profession identity.
- Synthetic IDs: `emi:/discovery/profession/<namespace>/<path>`.

**Limit:** the pages are linked to workstation/emerald/egg items, not separate synthetic profession `EmiStack` objects. Professions without item-representable job-site blocks can still show an informational page.

### Villager trades: potential-offer browser (26.3 data format)

**Feature reference:** [EMI Trades](https://modrinth.com/mod/emitrades). **No direct EMI upstream ticket.**

**Code:** new `recipe/EmiVillagerTradeRecipe.java`, `mixin/accessor/VillagerTradeAccessor.java`, `recipe/EmiVanillaDiscovery.java`, `emi.mixins.json`.

- Introduce the `emi:villager_trading` category using EMI's `EmiRecipeCategory`, `EmiRegistry.addCategory()`, `addWorkstation()` and `addRecipe()` methods. It is browsable from emeralds, villager eggs and wandering-trader eggs.
- The 26.3 game uses **registry-backed `TradeSet` and `VillagerTrade`** entries. Where the **client actually has** `Registries.TRADE_SET`, enumerate each profession's available levels 1–5 and wandering-trader trade groups. No static pre-26.3 `VillagerTrades` arrays or outdated addon classes are used.
- Access template cost/output fields through a narrow **read-only** Mixin accessor; never modify trade generation, offers, entity state or the actual merchant GUI. Render first/second payment items, output template, profession and unlock level. Display literal item counts when the price provider is `ConstantValue`.
- Show an explicit warning that offers are *potential*: random trade selection, predicates, discounts, demand, loot functions and data-pack/server state can modify prices and outputs. No probability is claimed; no recipe-tree cost path, inventory fill or crafting automation is offered (`supportsRecipeTree() == false`).
- If the trade-set registry isn't available to a remote client, **don't fabricate catalog entries**. The category and its source-aware limitations remain in place, but accurate dedicated-server coverage will require a negotiated server-side sync protocol and a separate compatibility review.
- Excluded/disabled item stacks are not registered as synthetic trade outputs or costs.
- Synthetic IDs: `emi:/discovery/trade/<namespace>/<profession-or-group>/<level>/<index>`.

**Developer note:** `EmiVillagerTradeRecipe` and `EmiVanillaDiscovery` are internal implementation classes; do not treat their Java signatures as new public integration hooks. For third-party categories and recipes, use EMI's existing `EmiPlugin#register(EmiRegistry)` extension point. The accessor targets the 26.3 `VillagerTrade` data fields `wants`, `additionalWants`, `gives` and is read-only. It is a runtime compatibility surface, not an ABI change.

### Info-page resilience

**Internal finding; no known upstream ticket.** **Code:** `api/recipe/EmiInfoRecipe.java`.

- Clamp computed text-row capacity to zero on extremely short recipe displays, avoid adding scroll controls when no lines fit, and guard `PageManager.scroll()` against division by zero. This hardens existing EMI info pages (including JEI-provided and datapack info recipes), not only the new discovery pages.
- No method signatures or normal-height behavior intentionally changed. Verify with short window heights and higher GUI scale before release.

### Loot and ore generation — researched, **not** silently implemented

- [EMI Loot](https://modrinth.com/mod/emi-loot) supports chest, block, mob and fishing loot tables, chance groups, and several conditions. It needs server-side evaluation of loot functions, entity/block/tool contexts and sync for accurate dedicated-server results. Fabric/Forge/NeoForge cannot safely infer current server loot from client item registries. **No percentage data, auto-generated loot recipes or packet protocol is shipped in Dev 3.**
- [EMI Ores](https://modrinth.com/mod/emi-ores) uses datapack world-generation configuration to report height distribution, vein size, attempts and biome restrictions, not fixed Y-level tables. Its code is licensed **LGPL-3.0-only**, so we have **not** copied it into PF-EMI's MIT source. Accurate multiplayer support requires access to that server's active configured/placed features and biome data. The worldgen analyzer and negotiated sync remain implementation work, not a completed feature.
- [JEED](https://www.curseforge.com/minecraft/mc-mods/just-enough-effect-descriptions-jeed) is **All Rights Reserved** on CurseForge. We use only its public localization convention, not its descriptions/code. EMIffect, EMI Loot, EMI Enchanting, EMI Trades and EMI Professions list MIT licenses; **no third-party source, art or assets were copied or derived in this pass**, so no bundled third-party license additions are necessary.

### Developer API / ABI impact — cumulative 1.1

| Surface | Dev 3 impact | Release caveat |
| --- | --- | --- |
| `EmiPlugin`, `EmiRegistry`, `EmiRecipe`, `WidgetHolder` | No descriptor, interface method or type change | Existing EMI API routes reused; test compiled addons later |
| `EmiInfoRecipe` | Internal paging safeguard, same constructors and method signatures | Observable behavior on zero-row layouts changes from possible exception to no scrolling |
| `EmiVanillaDiscovery`, `EmiVillagerTradeRecipe` | New implementation classes **outside** `dev.emi.emi.api` | Internal only; not promised as a supported extension ABI |
| Read-only Mixin accessor | One new client Mixin target for `VillagerTrade` | Verify transformed target on all three 26.3 loaders; no game rules or trade mutation |
| Pack data / language files | Added opt-in effect `.description` / `.desc` and enchantment `.desc` lookups; English UI labels | Resource-pack authors can override text normally; missing keys are ignored |
| Config / networking / saved data | No additions in Dev 3 | Server sync for loot, ore and missing trade tables is **not implemented** |
| Existing 1.1 API | Dev 2's opt-in `EmiPartialConsumptionRecipe` + `PartialInput` remain | Additive public API requiring ABI review before release |
| Java / loader / Minecraft | Still Java 25, MC 26.3, Fabric/Forge/NeoForge | No builds yet; cannot claim loader or binary compatibility |

**Interaction with other addons:** These features are integrated into the original EMI registration lifecycle and may overlap pages created by EMIffect/EMI Enchanting/EMI Trades if versions compatible with 26.3 are loaded simultaneously. Duplicate-category and duplicate-information coexistence must be checked in the 1.1 compatibility matrix before release; Dev 3 does **not** unilaterally disable third-party addons.

### 1.1 release test gate additions (deferred by request)

1. Build Java 25 on all three loaders and verify the accessor target fields / Mixin transforms and output-JAR integrity; compare public API bytecode with the previous release.
2. Open potion, splash potion, lingering potion and tipped arrow recipe/use pages; verify effect mapping, localization fallback and resource-pack overrides; disconnect/reload without stale entries.
3. Check several enchanted books, modded enchantment registries, curses, incompatible sets and supported items against actual in-game metadata; ensure disabled-tag entries remain excluded.
4. Check all 13 normal villager professions, job-site blocks, workstation pages, all five trade levels, wandering traders and dynamic-cost trades; compare integrated-world data against actual merchant offers.
5. Join a remote server without the trade-set registry and confirm graceful omission (no crash or misleading entries); assess the dedicated-server data-sync design **separately** before adding packets.
6. Exercise very short Info recipe screens to confirm the zero-line paging fix; inspect render order and tooltip behavior with JEI and third-party plugin info pages.
7. Re-test all Dev 1/Dev 2/v30 behaviors and real compiled addons with an ABI checker. No issue is claimed closed upstream merely because a related PF-EMI change exists.

---

## Unreleased 1.1 — development pass 2 (approved issue work)

This pass builds on all previous 1.1 changes; the distribution remains a **single cumulative patch**. No Gradle builds or runtime launch tests were performed. Features are recorded as *implemented in source*, not as fixed or closed in upstream EMI's tracker.

### UI: configurable placement for EMI and Recipe Tree buttons

**Upstream:** [EMI #1259](https://github.com/emilyploszaj/emi/issues/1259), with history in [#689](https://github.com/emilyploszaj/emi/issues/689).  
**Source:** `EmiConfig`, new `EmiButtonAnchor`, `EmiScreenManager.positionEmiButtons()`; English localizations.

- New `ui.emi-buttons-anchor` setting: `bottom-left` (the original default), `search-left`, `search-right`, `top-left`, and `top-right`. The top anchors position the buttons immediately outside the corresponding left/right sidebar, when that space exists.
- New `ui.emi-buttons-offset` setting for horizontal and vertical GUI-pixel offsets, using EMI's existing `IntGroup` setting mechanism. Offsets are bounded when applied so an invalid configuration cannot move buttons arbitrarily far offscreen.
- Existing visibility settings still decide whether each button is shown. Hiding the configuration button now moves the Recipe Tree button into the first slot rather than leaving an unused gap. Button coordinates are calculated when the screen widgets are set up, not by modifying them each render tick.
- The default configuration preserves existing button locations (2 px and 24 px from the left, 22 px above the bottom edge).

**Compatibility:** Additive config fields/keys; existing configs retain their old layout. No change to the EMI plugin API, input bindings, or button click actions. UI tests are required with custom container screens, narrow resolutions, visible/hidden sidebars, and 1–4 GUI scales. Issue #1259 is implemented locally in a conservative form, not closed upstream.

### Accessibility: searchable inventory outline

**Upstream:** [EMI #1262](https://github.com/emilyploszaj/emi/issues/1262).  
**Source:** `EmiConfig`, new `InventoryHighlightStyle`, `EmiScreenManager.renderSlotOverlays()`; English localizations.

- `ui.inventory-search-highlight` now offers `dim` (original behavior; still the default), `outline`, or `dim-and-outline`.
- When inventory search highlighting is active, matching nonempty slots can receive a bright inner border with a dark outer border. This provides a high-contrast shape cue that remains visible on dark inventory textures instead of depending on a dark translucent overlay alone.
- The search filter, recipe tree highlights, inventory slot contents, and all non-search rendering retain existing behavior.

**Compatibility:** Config-only new option; no public plugin API changes. Needs in-game contrast, focus, overlay and shader tests; not marked as verified for every GUI mod.

### JEI compatibility: preserve rich tooltip lines

**Upstream:** [EMI #1255](https://github.com/emilyploszaj/emi/issues/1255), related duplicate [#1256](https://github.com/emilyploszaj/emi/issues/1256).  
**Source:** `JemiTooltipBuilder`.

- The 26.3 port already implements the methods named by #1255: `IRecipeExtrasBuilder.addDrawableWidget()`, `addAnimatedRecipeArrowWidget()`, and `ITooltipBuilder.getLines()`. Recreating these methods was unnecessary.
- The existing `getLines()` implementation nevertheless returned *text lines only*. It silently dropped data-backed `TooltipComponent` entries (such as rich fluid or item tooltips) even though these were retained for rendering. The builder now maintains an ordered `Either<FormattedText, TooltipComponent>` list as JEI expects, and returns that list without discarding rich entries.
- `clear()` now resets the builder's own tooltip lists consistently. Non-`Component` text is converted to displayable text while retaining the original `FormattedText` value in the rich list.

**Compatibility:** The existing JEI method descriptors and exposed EMI API are unchanged; returned tooltip content is more complete. A compatible JEI runtime and mods exercising rich tooltips are required to confirm the bug is resolved on 26.3. No JEI APIs were copied or redefined.

### Recipe tree: opt-in capacity-based partial consumption estimates

**Upstream:** [EMI #1247](https://github.com/emilyploszaj/emi/issues/1247).  
**Source:** new `dev.emi.emi.api.recipe.EmiPartialConsumptionRecipe`, `MaterialNode`, `TreeCost`.

- Added a narrow, optional EMI-side interface for recipes with inputs representing a resource that is gradually consumed, such as a spool containing 1,000 units of wire.
- Implementations return `getPartialInputs()` with `PartialInput(inputIndex, capacityUnits, usedUnitsPerCraft)`. A recipe using 50 wire units from a 1,000-unit spool can now describe that relationship explicitly. The recipe-tree cost calculation derives the required number of full physical items for a batch instead of charging one for every individual craft.
- Without that interface the bill of materials uses the original logic. There is **no heuristic detection** based on item IDs, damage, NBT, or arbitrary remainders. This follows EMI's abstraction-first approach and avoids mod-specific assumptions.
- Partial-input requirements cannot be treated as infinite reusable catalysts. The tree also avoids blindly adding their per-craft `getRemainder()` items as surplus resources: this would otherwise create phantom leftover items in capacity-based estimates.
- Physical item remainder values and actual item transfer/crafting are **not changed**. Inventory progress for partially depleted physical objects is approximate (it counts whole objects, not remaining internal units). Multiple independent branches can still have imperfect resource pooling. JEI-only recipes cannot opt in unless an adapter/plugin exposes the EMI contract. This is deliberately scoped to avoid changing gameplay.

**Example for mod developers:**

```java
public class WindingEmiRecipe implements EmiPartialConsumptionRecipe {
    // All ordinary EmiRecipe methods still apply.
    @Override
    public List<PartialInput> getPartialInputs() {
        // Inputs[0] contains one 1,000-unit spool; one recipe consumes 50 units.
        return List.of(new PartialInput(0, 1000, 50));
    }
}
```

The example is illustrative and not a standalone recipe class. `inputIndex` refers to the zero-based position in `EmiRecipe.getInputs()`, and `usedUnitsPerCraft` is the total consumption represented by that input entry per craft, not a percentage. Entries must have nonnegative indices and positive unit values.

**API/ABI impact:** **New additive public type** `EmiPartialConsumptionRecipe` and its nested record `PartialInput`, with a default `getPartialInputs()` implementation. **No methods were added to or removed from `EmiRecipe`**, no existing method descriptors were altered, and existing plugins are not required to opt in. The new API requires explicit addon adoption. Compatibility is reasoned from source structure and has **not been binary-tested**. Review the long-term API contract with plugin authors before tagging a stable 1.1 release; #1247 is only *partially addressed* because the general resource-pooling problem is broader than this estimate.

### Source review, licensing and scope

- Reviewed the feature descriptions and licensing of [EMI++](https://modrinth.com/mod/emixx), [Reliable EMI](https://github.com/evanbones/Reliable-EMI), and [EMI Trades](https://github.com/Prismwork/EMITrades). No third-party source code was copied into this pass. Attribution to the original EMI project remains in its existing `LICENSE`.
- Villager trade browsing is a candidate for a later 1.1 development pass. These addons target different Minecraft versions; moving their implementations straight into 26.3 would be speculative without inspecting the 26.3 trading API and loader lifecycle. The present changes do not register trade categories or alter the recipe sync protocol.
- Existing JEI 30 bridge code was used instead of bypassing EMI's `WidgetHolder` or custom-drawing through Minecraft where EMI already has drawing primitives. No global warning filters or unrelated refactors were added.

### Additional release test requirements (not executed)

Check all anchors with both buttons shown, each button hidden, centered and sidebar-aligned search, sidebar changes, and small screen resolutions. Test inventory outlines with dark/light resource packs and screen overlays. Test rich JEI tooltips with `TooltipComponent` values. Test partial-consumption calculations at 1, 8, 20 and 21 crafts for the 50/1,000 spool example (expected 1, 1, 1, 2 full spools) and compare against unchanged non-opt-in recipe trees. Test all three loaders and externally compiled EMI addons before declaring API/ABI compatibility.

## Unreleased 1.1 — development pass 1 (after v30)

### Fix: shift-fill may clear a crafting grid instead of filling it

**Related upstream report:** [EMI #1191](https://github.com/emilyploszaj/emi/issues/1191), related PR [#1194](https://github.com/emilyploszaj/emi/pull/1194).  
**Affected code:** `EmiRecipeFiller.getStacks()` and `EmiRecipeFiller.clientFill()`.

- Shift-fill requests `Integer.MAX_VALUE` crafts. `amount + batchesAlreadyPresent(...)` previously used `int` math, so adding a positive existing-batch count could overflow to a negative number. A negative request then produced invalid stack counts or an aborted fill.
- The requested amount is now calculated in `long` and clamped to `[0, maxAmount]` **before** converting back to `int`.
- The available-craft calculation now checks slot capacity **for each destination ingredient slot**, rather than applying one stack-size bound to the total number of identical inputs. This avoids overfilling slots and needlessly penalizing recipes that put the same ingredient in multiple slots.
- `clientFill()` now validates the complete destination slot mapping before performing the first inventory click. A third-party handler returning a missing slot or too few slots is rejected before it can clear the current grid. This does not fully make network-driven inventory filling transactional.

**Developer impact:** Existing `StandardRecipeHandler` implementations retain the same contract and public methods. Invalid/malformed slot mappings now fail earlier without clearing items. Requests that exceed actual slot capacity are capped to legal per-slot counts. No packet format or crafting recipe behavior was intentionally changed.

**Verification:** Source review and targeted arithmetic/layout checks only. Needs live tests with Shift+Fill, shaped and shapeless recipes, repeated ingredients, odd stack limits, recipe conflicts, and both integrated/dedicated servers. Upstream #1191 is **addressed locally but not yet runtime-confirmed**. Issue [#1261](https://github.com/emilyploszaj/emi/issues/1261) (intermittent partial fills) remains open for reproduction: these guards may help one failure mode, but do not prove a fix for that issue.

### Fix/QoL: search grouped ingredients correctly

**Related upstream report:** [EMI #1258](https://github.com/emilyploszaj/emi/issues/1258) (search result pollution reported with EMI++ installed).  
**Affected code:** `EmiSearch.SearchWorker`.

- Search previously examined only ingredients containing exactly one `EmiStack`. Visible ingredient groups with multiple alternatives were silently skipped when any search text was present.
- Search now retains one group entry when at least one member matches the **entire compiled query**. This avoids duplicating the same group for every matching member and respects `AND`/`OR`/negation as applied to a single matching alternative.
- We did **not** change tag formation, the index, group ownership, or any third-party addon API; this is a conservative correction inside the existing search filter.

**Developer impact:** No Java API or ABI changes. Integrations that submit multi-stack `EmiIngredient` entries as a search source can now have those entries discovered by text search. This is observable behavior, not a new registration hook.

**Verification:** Source-level checks only. This is **not** claimed as a full fix for #1258, which has an addon-specific reproduction on another game version. Test item groups, tag groups, mod-prefixed queries, negations, and EMI++ or Reliable EMI together before release.

### Fix: stale search workers can publish mismatched query metadata

**Internal finding; no confirmed upstream ticket.**  
**Affected code:** `EmiSearch.apply()` and `EmiSearch.SearchWorker`.

- The shared `compiledQuery` field used to be overwritten by each background worker as soon as it parsed its query, even when a newer worker had already replaced it. The existing worker-identity guard protected the displayed stack list, **but not the accompanying query state**.
- The worker now holds its own compiled query and publishes that query together with the matching filtered list inside `EmiSearch.apply()`, only if it is still the current worker.

**Developer impact:** No public method signatures, config keys or network behavior changed. Other components reading `EmiSearch.compiledQuery` should now see the query corresponding to the latest accepted filtered search results rather than a potentially stale worker's query.

**Verification:** Static concurrency review only. Needs a fast typing/backspacing stress test in a large pack and across recipe reload/disconnect boundaries.

## Retained from v30 — included in the 1.1 cumulative patch

These are retained **without reverting or reimplementing** them. See the archived master `CHANGELOG.md` for the original v30 notes.

| Area | Carried-forward implementation | Related tracker |
| --- | --- | --- |
| Modpack control | Honor `#c:disabled_from_recipe_viewers` across supported registries (including corresponding block items) | [#1226](https://github.com/emilyploszaj/emi/issues/1226) |
| Tag handling | Keep tags empty when every candidate is disabled instead of restoring excluded members | #1226 related |
| Recipe reload | Guard cross-session asynchronous reloading, stale worker state and workstation index publication | See v30 notes and [#1270](https://github.com/emilyploszaj/emi/issues/1270) (related, not verified equivalent) |
| Plugin compatibility | Skip invalid third-party recipes with null categories rather than failing the whole bake | [#1218](https://github.com/emilyploszaj/emi/issues/1218) (related) |
| Fuel browser | Hover the fuel display to see burn duration in seconds, in addition to existing smelt totals | PF-EMI QoL |
| Creative index | Avoid dereferencing failed creative-tab population results a second time | Internal finding |

The v29 26.3 fuel-discovery fix (all 18 built-in vanilla cooking-time providers, including wood items) is **part of the uploaded baseline** and continues to be present. Its behavior is documented in `CHANGELOG.md`.

## API / ABI compatibility (developer-facing)

| Surface | Status in this development pass |
| --- | --- |
| `dev.emi.emi.api.*` Java source, including plugin and handler interfaces | **Unchanged** relative to the uploaded 26.3 baseline |
| Loader-specific public API sources | **Unchanged** |
| Public Java classes, method names/signatures in the added changes | **No intended changes** |
| JEI bridge | No new JEI interface implementation or JEI dependency requirement |
| Minecraft / loader / Java target | No change: Minecraft 26.3, three loaders, Java 25 |
| Pack data and tags | v30 adds the recognized `c:disabled_from_recipe_viewers` semantics; no new Java API |
| Saved files, configuration keys, network packet IDs/codecs | **Unchanged** |
| Observable internal behavior | Ingredient grouping in search, fill-request clamping, slot-validation failure behavior, and current-search metadata publication changed as described above |
| Binary compatibility | **Not yet verified**. Bytecode ABI comparison and third-party compiled-addon tests are required once builds resume for the 1.1 release. Source-level checks alone cannot prove ABI compatibility. |

### Test gate for 1.1 (deferred by request; no builds now)

1. Compile all three 26.3 loaders with Java 25; capture warnings/errors without hiding failures.
2. Verify existing public API/ABI against the last working artifacts with an ABI comparison tool and compiled test addons.
3. For Fabric, NeoForge and Forge, exercise normal and Shift+Fill from empty and pre-populated crafting grids, including recipes with overlapping inputs and nonstackable catalysts.
4. Verify search matches with ordinary item entries, grouped entries, tags, `@mod`, quoted terms, `-negative` terms and OR expressions. Rapidly change search text and rejoin a remote server.
5. Test plain EMI, JEI integration (when a compatible version is available), EMI++, Reliable EMI and a deliberately faulty plugin separately. Never assume these addons all support 26.3.
6. Re-test singleplayer and multiplayer recipe sync, fuel browser entries, and `c:disabled_from_recipe_viewers` after data-pack reloads.

## Reviewed but not changed during development pass 1 (historical)

- [#1255](https://github.com/emilyploszaj/emi/issues/1255) / [#1245](https://github.com/emilyploszaj/emi/issues/1245): JEI interface incompatibilities must be assessed against a **specific compatible 26.3 JEI API/runtime**. The 26.3 code already implements `IRecipeSlotView.getAllIngredientsList()` and related methods. No unsupported cross-version shims were added.
- [#1262](https://github.com/emilyploszaj/emi/issues/1262): higher-contrast inventory highlighting could improve accessibility, but default UI changes should be tested and configured before changing visuals.
- [#1259](https://github.com/emilyploszaj/emi/issues/1259): configurable EMI/recipe-tree button anchoring is a user-interface feature, not a small compatibility fix. Deferred until the layout is agreed.
- [#1247](https://github.com/emilyploszaj/emi/issues/1247): partially consumed recipe-tree ingredients need explicit semantics or an API design; **do not** infer amounts from remainders without agreement.
- [#1263](https://github.com/emilyploszaj/emi/issues/1263): deleted items still visible. Reproduction and loader/addon interactions still need inspection; not marked fixed.
- Villager trades, major index/sidebar changes, or new recipe protocols: intentionally withheld from this update to avoid an unreviewed breaking redesign or incompatible public hook.

## Source and distribution policy

- 1.1 is a **single cumulative patch**. Apply to the uploaded 26.3 baseline, not as a sequence of v30 + development fixes. v30 changes are already included in the 1.1 patch.
- Patch files are at the ZIP root with their original paths. No nested project directory, build caches, logs, release JARs, duplicated source archives or automated build step.
- Upstream EMI contribution guidelines are treated as a quality/compatibility guide; **this is a PF-EMI-local change**, not a submitted upstream pull request. Upstream authors discourage LLM-generated contributions, and no such PR is being made.
