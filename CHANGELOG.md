# PF-EMI — Master Changelog and Patch History

## Unreleased — PF-EMI 1.1 Dev 3.3 (source-only correction)

Fix addon-style feature discoverability: expose separate Status Effects, Enchantments, Villager Professions and Villager Trading categories instead of placing everything in the generic Info category. Register all status effects (including those without potions), use built-in job-site registries, and show an explicit notice when a server has not shared its trade registry. Add per-section registration counts and isolate malformed entries so one error cannot silently halt an entire discovery loop. Retain Dev 3.2 and older cumulative changes. No public Java ABI change; four new synthetic category IDs are observable. See `CHANGELOG-1.1.md` for implementation, limitations, API impact and validation requirements. The Fabric user test of Dev 3.2 showed features were not usefully discoverable; Dev 3.3 is not yet built or tested.


**Current source target: Minecraft 26.3 (Fabric, NeoForge, Forge)**  
**Ongoing 1.1 development (Dev 3: effect, enchantment, profession and potential-trade information):** see [`CHANGELOG-1.1.md`](CHANGELOG-1.1.md) for the cumulative unreleased feature/fix branch.  
**Last finalized historical patch notes: v30 — October 27, 2026**

This is the **historical master patch-note archive**. The ongoing 1.1 work is maintained separately in `CHANGELOG-1.1.md`. The short release history is directly below; the collapsible reference archive at the end preserves the original patch-note text for auditability. The historical apply/extract directions in that archive are **obsolete**: this is a complete 26.3 source tree, not a sequence of patches. The archived 1.21.x, 26.1 and 26.2 notes do not imply their source targets remain supported here.

## Minecraft 26.3 — active history

| Patch | Main change |
| --- | --- |
| **v30** | Added `#c:disabled_from_recipe_viewers` support across registered item/block/fluid/other EMI registries; fixed fully disabled tag-value resurrection; hardened cross-session async recipe indexing and unload state; ignored invalid third-party null categories; added fuel burn-duration tooltips and creative-tab failure guards. Patch-only distribution. |
| **v29** | Fuel browser recognizes all 18 vanilla `minecraft:cooking/time_*` reference providers, standard furnace burn times and literal positive constants. Fixes omitted wooden fuels on all three loaders. Arbitrary dynamic/modded provider values are not fabricated. |
| **v28** | Standardized `1.0+26.3+{fabric,neoforge,forge}.jar` outputs, launcher pause/no-Explorer behavior, stale-release checks, and narrowly scoped legacy JEI compiler annotations. |
| **v27** | NeoForge waits for recipe-map readiness before reload; optional JEI mixin only activates when appropriate; Java 25 Gradle native access flags, warning analysis and specific JEI annotations. |
| **v26** | Forge recipe-sync fallback through registry-aware `Recipe.DIRECT_CODEC` when `STREAM_CODEC` roundtrip fails; Fabric avoids duplicate reload signals; brewing reagent predicates preserved. |
| **v25** | Fabric creates its recipe lookup from received synchronized recipes; Forge combines and applies received recipe chunks; NeoForge moves axe transformation compatibility outside the Mixin package. |
| **v24** | Removed obsolete Forge brewing imports after migrating to shared brewing discovery. |
| **v23** | NeoForge compilation compatibility follow-up for 26.3. |
| **v22** | Removed invalid `PotionBrewing$Mix` class access-widener and Forge access-transformer entries. |
| **v21** | Updated code paths for 26.3's reloadable recipe registry / `RecipeMap` API. |
| **v20** | Removed obsolete tool subclasses and mixins; adapted screenshot/GUI tooltips, key input, inventory prediction, bone meal, JEI contexts and compostable data components. |
| **v19** | Initial 26.3 target migration: Fabric API/NeoForge/Forge/JEI coordinates, SDL input, Renderpearl GPU types, data-driven brewing and cooking-fuel components. |

### v30 implementation notes (2026-10-27)

- **Pack-author control:** use `#c:disabled_from_recipe_viewers` in item/block/fluid registry tags to disable stacks entirely, including tag ingredients and recipes. This differs from `#c:hidden_from_recipe_viewers`, which only removes entries from the visible item index. Block tags also apply to matching block-item stacks. Only registries that EMI indexes through registered adapters can be affected.
- **Tag integrity:** fully disabled tags remain empty rather than accidentally restoring original values after filtering; unusable tags are removed from the selectable tag list.
- **Rejoin/reload:** synchronization readiness flags are updated atomically; a worker running after world closure settles to waiting rather than an erroneous reloading state; outdated asynchronous recipe workers cannot clear newer workers or publish old workstation indexes.
- **Third-party safety:** recipes without a category no longer abort an entire bake; failed creative-tab item population is skipped rather than repeatedly dereferenced.
- **Fuel QoL:** hovering the animated fuel flame shows exact burn duration in seconds; existing smelt-count labels remain unchanged.
- **API/ABI:** no files under `dev/emi/emi/api/` or platform public API directories were altered; no new required mod, loader, config parameter, plugin hook or network message was introduced. A source-level public-API check passed, but runtime/bytecode ABI compatibility must still be tested with built Java 25 artifacts and third-party integrations.
- **Out of scope:** creative-tab sidebars, stack-group editing, trade browsing, and JEI API migrations require more UI/runtime verification. No upstream PR has been sent. Changes are local to PF-EMI.

#### Upstream issue and extension review

- [EMI #1226](https://github.com/emilyploszaj/emi/issues/1226) — implemented the requested `c:disabled_from_recipe_viewers` semantics in this 26.3 fork (source-only verification). This is intentionally not a new public Java API.
- [EMI #1218](https://github.com/emilyploszaj/emi/issues/1218) — prevents one obvious null-category failure route while baking third-party recipes; does **not** establish that all external null-data reports are resolved.
- [EMI #1270](https://github.com/emilyploszaj/emi/issues/1270) — audited connection-reload state and corrected an async worker race; the exact upstream reproduction has not been independently confirmed here.
- [EMI #1255](https://github.com/emilyploszaj/emi/issues/1255) and [#1245](https://github.com/emilyploszaj/emi/issues/1245) — JEI binary compatibility needs a separate supported JEI installation/test matrix; not marked fixed.
- [EMI #1258](https://github.com/emilyploszaj/emi/issues/1258) — searching may be affected by addon stack-group behavior; not patched without a reproducible 26.3 case.
- [Reliable EMI](https://modrinth.com/mod/reliable-emi) and [EMI++](https://modrinth.com/mod/emixx) — their workstation tabs, editable groups, creative tabs and villager trade pages are useful design references, but require UI, accessibility and data-provenance validation before bringing into core. Code was not copied from these addons.
- EMI `CONTRIBUTING.md` requests stable small diffs, preserving ABI, no incidental API expansion, and strongly cautions against submitting LLM-generated source. This patch is only for PF-EMI; no upstream submission is implied.


### 26.3 release behavior and constraints

- **Build:** Windows `build.bat` (or `--forge-only`), Linux `./build.sh` (or `--forge-only`). Root Gradle builds Fabric+NeoForge; independent ForgeGradle 7 builds Forge. Java 25 required. The consolidated scripts replace the older `build-26x*`, per-version launchers and Forge-specific batch files. No automatic Explorer or editor launch.
- **Artifacts:** Only actual loader JARs are copied to `release/`. `1.0` is a distributable filename prefix; internal versions retain the project's distinct mod version.
- **Fuel values:** v29 recognizes 18 vanilla 26.3 cooking-time provider IDs, with normal-furnace ticks (including vanilla's 4001-tick dried kelp block). Arbitrary contextual providers, server-side datapack overrides and exact custom modded values are not guaranteed without synchronization.
- **Recipes:** v26 added Forge codec fallback to prevent losing supported shaped/tagged recipes on network serialization. A nonzero omission count is still a bug to investigate; packet decoding can also fail when a remote registry is missing entries.
- **Fabric:** The synchronized-recipe event is authoritative. Vanilla-only remote servers that provide no such recipe data have not been established as complete.
- **Brewing:** 26.3 datapack-driven recipes account for input and reagent potion predicates; missing display alternatives are skipped and logged rather than guessed.
- **Other data-driven categories:** Context-dependent compost chances, third-party block transformations and arbitrary recipe/ingredient providers still require specialized handling. The GUI non-batched render path should be profiled in large modpacks.
- **Warnings:** Legacy JEI deprecation, Gradle plugin deprecation and some Minecraft/third-party runtime warnings may remain. Suppressing warnings would not mean the underlying problems are fixed.
- **Testing status:** Individual historical patch notes describe their own limited validation; a successful Java 25 build, all three clients and dedicated multiplayer need verification in the user's environment. This source clean-up did not rebuild any binaries.

## Earlier port history — archived (not build targets of this checkout)

### Minecraft 26.2 (v12–v18)

- **v12:** updated 26.2 dependencies and resource formats; moved toward its official-name input, rendering and loader APIs.
- **v13–v14:** shared-source migration / compiler corrections, including GUI rendering, effects, screenshot pixels, text and chat APIs.
- **v15–v16:** NeoForge foreground GUI event and Forge source compile compatibility fixes.
- **v17:** three-loader startup fixes based on game runtime logs.
- **v18:** NeoForge inventory coordinate / hover alignment fix, retained in the 26.3 port.

### Minecraft 26.1 / 26.1.2 (Forge fixes v6–v11)

ForgeGradle 7 initial port, native GUI input hooks, Mixin compatibility checks and manifest validation, packet guards and server-client chunked recipe synchronization. Older version-specific build configurations and apply scripts have been removed from this 26.3-only checkout.

### Minecraft 1.21.3–1.21.11 and older passes

Historical versions of the Fabric, NeoForge and Forge ports covered changing GUI tooltips, mixins, input APIs, loader registration, packet synchronization and data structures. Quilt was retired. References to old Yarn/refmap files, 1.21.x migration PowerShell scripts and old port applications are now historical only. This tree neither builds nor distributes the 1.21.x targets.

## Complete original patch-note archive (reference only)

The following sections reproduce the historical standalone patch notes in **one master file** to preserve implementation details without leaving dozens of files scattered around the project. Each item is collapsed by default. **Do not run historical patch-application steps against this 26.3 tree.**

### Previous main changelog (v19–v29; original text)

<details>
<summary>Expand original CHANGELOG.md</summary>

## v29 — Minecraft 26.3 complete vanilla fuel browser (2026-10-27)

- Fixed the all-loader EMI fuel page omitting most wood and other vanilla fuels after the 26.3 `COOKING_FUEL` data-component migration.
- Resolve all 18 default `minecraft:cooking/time_*` reference providers at their normal-furnace tick values, based on the Minecraft 26.3 `ContextIntProviders.bootstrap` source; retain direct constant values for modded fuels.
- Restored discoverability for wood logs/planks, wooden stairs and slabs, wooden tools, signs, boats, wool, bamboo, roots, and all other vanilla fuel item registrations.
- Avoid invented times for arbitrary third-party provider keys and server-specific data-pack overrides; precise custom provider support would require server-side synchronization.
- Preserved the working Fabric/NeoForge/Forge 26.3 ports, build conventions, release JAR naming, terminal pause, and no-Explorer behavior. Real Java 25 build and multiplayer verification still required.

## v28 — 26.3 release naming, Windows build controls, compiler warning annotations (2026-10-27)

- Release files follow the requested `1.0+26.3+{fabric,forge,neoforge}.jar` pattern. The `1.0` prefix is a distributable filename only, not a change to the mod metadata or Java sources.
- `build-26.3-all.bat` and `build.bat` run the correct 26.3 three-loader workflow. Direct `build-26x.bat` execution pauses after either success or failure; the wrapper pauses once on completion. Forge-only builds also remain open until a keypress.
- Removed automatic release-folder Explorer launches and automatic log-editor launches, and removed the dangerous old Forge 1.21.7 Modrinth profile install prompt from the 26.3 helper.
- Removed old per-26.3 artifact filenames at build start so stale artifacts cannot masquerade as new outputs. This does not delete files for unrelated Minecraft versions.
- Kept Java 25 native access configuration from v27, and narrowed documentation-backed Java compiler suppressions to intentional legacy JEI calls. Did not globally turn off Gradle warnings or forge any zero-warning claim.
- Updated Linux build scripts for 26.3 target and output parity, rather than continuing to build the obsolete 26.2 target.
- No binary rebuilt in this environment; Java 25 runtime testing remains required.

## v27 — 26.3 log lifecycle / optional integration cleanup (2026-10-27)

- NeoForge only starts EMI recipe reloads once its recipe map exists (avoids misleading multiplayer reload failures).
- Optional JEI PluginCaller mixin is registered conditionally instead of probing a missing JEI class.
- Gradle Java 25 native-library access configured for build launcher and daemons; no Minecraft launch flags changed.
- Retain legacy JEI drawable/arrow compatibility overrides with narrowly scoped compiler-removal annotations.
- Documented external multiplayer, loader, and remaining compatibility warnings in `PF_EMI_26.3_LOG_AUDIT_v27.md`.
- Build/runtime testing with Java 25 still required; this is not a prebuilt release.

## PF-EMI 26.3 — v26 recipe coverage and reload correctness (2026-10-27)

- Forge: recover previously unsyncable recipe definitions using registry-aware data-pack codec fallback with per-entry framing, roundtrip preflight and size limits; log exact encoded/fallback/omitted counts.
- Fabric: prevent duplicate EMI reloads by using the authoritative Fabric recipe event and separate tag events, not vanilla display-only recipe updates.
- All loaders: honor potion predicates on data-driven brewing reagents.
- Preserve working 26.3 UI and loader compatibility; see `PF_EMI_26.3_PREDEPLOY_FIX_v26.md` for test gates and unresolved limitations.

## 1.1.24-SNAPSHOT+26.3 — v25 runtime synchronization patch (2026-10-27)

- Fabric: build client recipe lookup from synchronized recipes; fix missing `minecraft:recipe` registry disconnect.
- Forge: assemble received recipe-sync chunks into an EMI RecipeMap; add transfer diagnostics.
- NeoForge: move direct-call axe transformation fallback outside the Mixin package.
- Preserve prior 26.3 v19-v24 compile fixes and the 26.2 NeoForge GUI alignment fix.

## PF-EMI 26.3 Port Candidate v19 (2026-10-27)
- Version and dependency retarget: 26.3, Fabric API 0.165.0, NeoForge 26.3.0.84, Forge 66.0.12, JEI 31.14.0.72.
- Migrate SDL keyboard and mouse constants, remove GLFW references, notify text input focus.
- Migrate Renderpearl GPU types and metadata, preserve NeoForge GUI alignment v18.
- Add dedicated 26.3 build entry point; runtime unverified.

### Tweaks
* Adjusted mixin notification logging to properly avoid printing when empty.

### Fixes
* Fix inconsistent crashes due to mixin order, typically on Fabric 1.19.2 or 1.20.1 #1171
* Fixed double applications of self-mutation mixins
- [26.3 v19] Migrate vanilla brewing recipe lookup to datapack RecipeType.BREWING across all loaders.
- [26.3 v19] Replace removed FuelValues API with CookingFuel component discovery and documented dynamic-provider limitations.

</details>

### 26.3 original notes

<details>
<summary>PF_EMI_26.3_FUEL_DISCOVERY_FIX_v29.md</summary>

# PF-EMI 26.3 — v29 fuel-page completeness fix

## Problem / root cause

Minecraft 26.3 no longer uses the old `FuelValues`-based lookup. Vanilla fuels
carry `DataComponents.COOKING_FUEL`, whose burn time is a `ResolvableInt`.
Almost all vanilla fuels refer to `minecraft:cooking/time_*` context providers,
but the v28 implementation only supported `ResolvableInt.Constant` and six
individual items. As a result, logs, planks and most other wooden fuels were
silently excluded from EMI's fuel category, on all three loaders.

## Change

The shared `EmiFuelDiscovery` implementation now recognizes **every one of the
18 `COOKING_TIME_*` reference keys** defined by `ContextIntProviders.bootstrap`
in the Minecraft 26.3 Forge-patched Mojang-mapped sources. The fallback values
are the **normal-furnace** (not blast/smoker) ticks, and preserve vanilla's
unusual 4001-tick dried kelp block. A constant-component burn time is read
verbatim if positive. Item enumeration and tag compression are unchanged.

| Burn ticks | Vanilla provider keys (`minecraft:cooking/…`) | Samples |
|---:|---|---|
| 50 | `time_bamboo`, `time_wool_slabs` | Bamboo |
| 67 | `time_wool_carpets` | Wool carpets |
| 100 | `time_dry_plants`, `time_wood_items_extra_small`, `time_wool` | Dry plants, wool |
| 150 | `time_wood_slabs` | Wooden slabs |
| 200 | `time_wood_items_large` | Some wood items |
| 300 | `time_roots`, `time_wood_blocks`, `time_wood_items_small` | Logs, planks, stairs |
| 800 | `time_hanging_signs` | Hanging signs |
| 1200 | `time_boats` | Boats and chest boats |
| 1600 | `time_coal` | Coal and charcoal |
| 2400 | `time_blaze_rod` | Blaze rods |
| 4001 | `time_dried_kelp_block` | Dried kelp block |
| 16000 | `time_coal_block` | Coal block |
| 20000 | `time_lava_bucket` | Lava bucket |

Reference mapping sourced from `net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.bootstrap` in the **actual 26.3 Forge Mavenizer patched Minecraft sources** supplied in the user's v26.3 archive, not from 1.21.x historical values. Existing EMI widgets display the number of standard recipes smeltable, based on `burnTicks / 200f`.

## Scope and non-goals

- **Works the same on all three loaders** because their `getFuelMapAgnos()` implementations already delegate to shared `EmiFuelDiscovery`.
- Unknown modded reference keys and overridden vanilla reference providers are **not** assumed to have universal numeric values. They require a synced provider registry / server-side burn-duration query for full correctness, especially on modded servers and with custom data packs.
- Forge-specific fuel event changes, potentially dependent on recipe type and server context, cannot be inferred from client-side `COOKING_FUEL` alone.
- Do not use `--warning-mode none` or suppress runtime warnings as a substitute for fixing compatibility.

## Validation performed

Static tests compare every `COOKING_TIME_*` constant and its 26.3 registered baseline value against the implemented switch. Item registration spot checks include `OAK_LOG`, `OAK_PLANKS`, `OAK_STAIRS`, and `OAK_BOAT`. ZIP layout, script naming and release target are checked. Java 25 and full Gradle dependency resolution are not available in this environment, so **JAR compilation and in-game tests are pending**.

## Required user acceptance checks

After a successful `build.bat` build, verify the EMI Fuel page on **Fabric, Forge, and NeoForge** with Minecraft 26.3 and check:

1. Oak Log, Oak Planks, Spruce Planks, Stripped Logs, Oak Stairs, wooden slabs and signs appear (possibly consolidated into equivalent-time tag groups).
2. Oak Log and Oak Planks show 300 ticks / **1.5 normal-furnace smelts**; wooden slabs show 150 ticks / **0.75 smelts**.
3. Coal/charcoal show 1600 ticks / **8 smelts**; lava bucket 20000 ticks / **100 smelts** (and bucket remainder).
4. Check modded items with literal cooking fuel components as well as items without fuel components.
5. Join a singleplayer world and a multiplayer server and verify EMI reloads correctly; open the fuel category again after each world transition.
6. Confirm release names remain `1.0+26.3+fabric.jar`, `1.0+26.3+forge.jar`, `1.0+26.3+neoforge.jar`. Terminal stays open after builds and Explorer is not auto-opened.

</details>

<details>
<summary>PF_EMI_26.3_BUILD_TERMINAL_CLEANUP_v28.md</summary>

# PF-EMI 26.3 — Build terminal / output cleanup v28

## Primary launcher

Double-click `build.bat` **or** `build-26.3-all.bat`. This calls the 26.3 backend once, builds Fabric + NeoForge followed by Forge, and requires a keypress before exiting regardless of success or failure. `build-26x.bat` is also safe to execute directly and pauses on its own. The Forge-only helper also pauses on every exit path. None launches File Explorer or Notepad automatically.

## Exact release names

The build emits playable shaded/remapped loader artifacts into the dedicated `release/` directory:

- `release/1.0+26.3+fabric.jar`
- `release/1.0+26.3+neoforge.jar`
- `release/1.0+26.3+forge.jar`

The sample image shows `1.0+26.2+...` because it depicts 26.2. These are **26.3** binaries. `1.0` is the **release filename prefix only**; version inside metadata remains the existing PF-EMI 1.1.24 snapshot lineage. Renaming the JAR does not change Minecraft compatibility and cannot make a 26.2 build work on 26.3.

Prior `PF-EMI-26.3*.jar` and the current-target `1.0+26.3+*.jar` outputs are removed before a new build to prevent stale JARs. Other releases are left untouched. Dev/API/sources JARs are never selected for distribution. On failure the script reports the failure and does not claim all three loaders passed.

## Warning policy

- Java 25's Gradle `java.lang.System::load` native-access warning was addressed in v27 with scoped `JAVA_OPTS` and `org.gradle.jvmargs`; retained in v28. This is a **toolchain flag**, not a log-suppression setting.
- Six legacy JEI adapter overrides received narrow `removal` annotations in v27. Four remaining legacy JEI *call sites* now receive only method-local `@SuppressWarnings("removal")`, with inline explanations. The `JemiPlaceable` adapter's known self-typed cast receives method-local `unchecked` suppression. No logic changed; **this removes compiler noise, not the underlying eventual JEI API migration requirement**. Future JEI versions may remove those methods.
- Gradle 9's "deprecated features were used" messages may still appear until Architectury/Loom/ForgeGradle or local script usages are migrated. `--warning-mode all` is the appropriate diagnostic when prioritizing those. This update does **not** use `--warning-mode none`, stderr redirection, or global log filters to hide genuine errors.
- Forge Mavenizer's initial `Cache miss! Stacktrace for Information Only` is dependency-preparation cache info, not a compile failure. It may disappear with a warm cache.
- Game-client WARN/ERROR records arising from Mojang authentication, Minecraft renderer, or public servers are not Gradle warnings and cannot be fixed safely by changing the build batch files. The v27 NeoForge EMI recipe readiness and optional JEI-mixin fixes are retained unchanged.

## Validation

- Reviewed the actual v27 26.3 source ZIP and the provided Gradle/runtime logs.
- Verified the v28 scripts' filenames and launcher/pause references statically and checked POSIX script syntax.
- No Windows cmd.exe and no Java 25 runtime available in this environment, so the three real 26.3 Gradle builds and client boots require testing on your Windows machine. No compiled JARs are included.

## Recommended post-build tests

On Windows with Java 25 selected: double-click `build.bat`, verify all three `1.0+26.3+*.jar` files are produced, confirm Explorer doesn't open, and press a key to dismiss the finished terminal. Repeat with a simulated build failure or after provoking a benign Gradle failure to ensure the window still pauses. Compare remaining warnings with the v27 log audit and run a singleplayer + vanilla multiplayer + third-party-server test on each loader before release.

</details>

<details>
<summary>PF_EMI_26.3_LOG_AUDIT_v27.md</summary>

# PF-EMI 26.3 — Log audit and targeted cleanup v27

**Date:** 2026-10-27  
**Baseline:** the uploaded `PF-EMI (26.3) (2).zip` (v26 source).  
**Scope:** singleplayer and public-multiplayer testing from NeoForge, Fabric, Forge, plus the Gradle build output.

## Changes included in this patch

1. **NeoForge recipe readiness:** Do not invoke EMI's resource-reload worker on tag updates until NeoForge has delivered a non-null recipe map. Do not request a reload from a null `RecipesReceivedEvent` map. This prevents the `Recipe Manager is null` failure visible twice on NeoForge when switching to public servers. It does **not** invent server recipes: servers without full recipe sync can still have incomplete EMI listings.
2. **JEI optional Mixin:** Remove the JEI-only `PluginCallerMixin` from the unconditional client mixin lists (common and Forge 26.3), and dynamically add it through the existing `EmiMixinPlugin` only when the JEI target class is available. This removes the absent-JEI class load attempt while retaining the plugin mixin when JEI is present. **Re-test with JEI actually installed before release** to ensure the optional plugin is discovered at bootstrap.
3. **Gradle Java 25 native access:** Set the Gradle daemon native-access flag in both projects, plus the wrapper launcher via local `JAVA_OPTS` in `build-26x.bat`. Does not change players' launchers or suppress a warning using log configuration.
4. **Legacy JEI adapter annotations:** The six compatibility overrides in `JemiRecipeExtrasBuilder` remain so older JEI plugin code works. Mark just those legacy overrides with `@SuppressWarnings("removal")`; modern widget methods remain intact. These are warnings for intentionally retained compatibility APIs, not changes to runtime behavior.

## Measured from uploaded runtime logs (WARN/ERROR log records, excluding stack trace lines)

| Loader | WARN | ERROR | PF-EMI-owned ERROR |
|---|---:|---:|---:|
| NeoForge | 190 | 91 | 2 (`Recipe Manager is null`) |
| Fabric | 102 | 67 | 0 |
| Forge | 47 | 37 | 0 |

The totals are **before** patch. They are not predictions of the next playtest's totals.

## Other warnings: attribution, risk, and action

| Message family | Origin | Interpretation / action |
|---|---|---|
| `Profile contained invalid signature for textures`, `Bad signature length`, malformed textures property | Mojang authlib, data from remote player profiles/server | Multiplayer sends empty, malformed, or invalid skin signature. No PF-EMI change fixes the remote data. Do not disable signature checks. |
| `Requested creation of existing team 'glow-GREEN'`, `Received packet for unknown team`, player info for unknown player, passenger/entity warnings | Minecraft client parsing remote server packets | Usually network-entity/scoreboard lifecycle mismatches seen on plugin-heavy servers. Reproduce on clean vanilla server before blaming EMI. |
| `hypixel_skyblock:*` missing models, invalid JSON item model, missing textures | Hypixel server resource pack | Third-party asset problem, not EMI model definitions. |
| `minehut:*` unknown sound or model; unknown host names containing stray quote/backslash | Remote server/connection client | Invalid server-provided sound/model or malformed address. Check literal server address when testing. |
| `minecraft:end_of_frame` requested post effect absent | Vanilla `ShaderManager` | Also occurs in all three isolated loader logs without a stack naming EMI. Don't add empty dummy shaders or suppress diagnostics to make it vanish. Verify against vanilla 26.3 baseline if needed. |
| `hypixel:hello`, `badlion:mods`, `vv:server_details` unregistered payload | NeoForge registry rejects remote optional custom payload | Expected interoperability issue with optional client channels; refusing to decode unknown payloads is appropriate. |
| Java `sun.misc.Unsafe` from Forge `UnsafeHacks` / NeoForge TypeResolver | Loader dependencies on Java 25 | Upstream libraries, not PF-EMI. Replacement/update must come from maintainers. No PF-EMI JNI workaround is appropriate. |
| LWJGL / JNA native access warnings in Forge launch | Java 25 / native library modules | JVM launch configuration can allow known modules if launcher permits it, but mod cannot safely force JVM flags. Not evidence of EMI performance overhead. |
| 10 `-Xlint:removal` messages from JEI APIs in each common/Forge compile | PF-EMI JEMI compatibility layer | Six method overrides intentionally retained and annotated. **Four remaining call-site warnings** need migration to the appropriate JEI 26.3 tooltip / transfer API in a separate tested compatibility update. Do not broadly suppress them. |
| Gradle 9 deprecated features / Forge Mavenizer `Cache miss! Stacktrace for Information Only` | Gradle plugins / normal ForgeGradle cached dependency preparation | Run with `--warning-mode all` to identify actual deprecated plugin/script API. Cache miss alone is not an error and improves on warm builds. |

## Validation status

Static source/JSON/file checks completed in this environment. **No Java 25 toolchain or Minecraft client was available**, so none of the three mod JARs were rebuilt or runtime-tested here. The ZIP includes source changes only, not a claimed working binary.

**Before deployment:** rebuild with `build-26.3-all.bat` using Java 25, compare build warnings; test singleplayer and vanilla multiplayer plus public servers on all three loaders; specifically test both **with and without JEI**; verify actual vanilla and modded recipe lists, reload/rejoin and search, GUI hotkeys, crafting transfer, and no new Mixin errors. With server-hosted recipe sync absent, verify explicitly what recipes remain available instead of assuming success from an empty log.

**Performance:** suppressing an unimportant warning seldom changes game load time. The meaningful optimization is avoiding invalid or duplicate reloads, including the guarded NeoForge path. Measure startup and join phases on identical warmed installs to establish actual speed changes.

</details>

<details>
<summary>PF_EMI_26.3_PREDEPLOY_FIX_v26.md</summary>

# PF-EMI 26.3 — Three-loader pre-release corrective patch v26

**Date:** 2026-10-27  
**Baseline:** 26.3 source with patches v19–v25 applied. Apply v26 over that source folder, not the 26.2 project.  
**Scope:** fixes confirmed in v25 singleplayer logs and one data-driven brewing fidelity gap.

## Changes

### Forge 66: synchronize all supported recipes, including tag-based definitions

The legacy `RecipeHolder.STREAM_CODEC` throws `UnsupportedOperationException: Ingredients can't be empty` for 272/2042 recipes in the v25 logs (boats, signs, armor stand, etc.). This build adds a second wire representation using Minecraft 26.3's `Recipe.DIRECT_CODEC` with registry-aware JSON ops. It is selected **only** when the old stream encoding fails an actual encode/decode roundtrip. Both representations are framed individually and preflighted before sending; malformed individual entries can be skipped without aborting the entire packet. Packet and string limits remain bounded.

Expected server log format: `Forge recipe sync: <encoded>/<total> recipes encoded; <fallbacks> required 26.3 data-pack codec fallback; no omissions`. A nonzero omission count remains a bug requiring inspection. The server-side roundtrip check does not guarantee the remote client has every relevant datapack registry; client decode failures are explicitly logged and skipped. No fabricated recipes are generated.

### Fabric: eliminate duplicate resource reload requests

The client now accepts recipe synchronization **only** from Fabric's `ClientRecipeSynchronizedEvent`; the old `ClientPlayNetworkHandlerMixin.handleUpdateRecipes` callback was a second overlapping recipe signal. The event now calls `reloadRecipes()` alone: tag updates continue to come from configuration/play tag listeners, rather than being forged by the recipe event. This removes the duplicated join-time EMI reload seen in v25 and preserves reconfiguration and datapack sync signaling.

Note: vanilla-only remote servers without Fabric recipe synchronization have not been verified; complete recipe support remains dependent on receiving recipe data from the server.

### All three loaders: preserve brewing reagent predicates

The data-driven `PotionIngredient` can restrict both the input and reagent with `potion_contents`. v19-v25 only expanded the input predicate and discarded the reagent predicate. v26 enumerates **actual matching reagent ItemStacks** (including potion variants) and represents them as an EMI ingredient. A recipe whose reagent predicate resolves to no valid display variants is explicitly skipped and logged, rather than incorrectly displaying arbitrary potion reagents.

### Unchanged behavior

- NeoForge v18 screen coordinate fix.
- Core 26.3 SDL, Renderpearl, JEI, and recipe-registry migrations.
- Common 26.3 tool transformation, fuel and composting fallback behavior.
- Existing build scripts, loader dependencies and artifact naming.

## Validation and release gate

Static source and ZIP checks completed. A full Gradle Java 25 build was not possible in this environment; these changes are **UNVERIFIED in Minecraft**. Re-run `build-26.3-all.bat` and verify startup, inventory, browsing, search, recipe transfer, favorite interactions, world reload and clean shutdown on Fabric, NeoForge and Forge.

**Critical Forge test:** Check `acacia_boat`, `acacia_sign`, `andesite_stairs` and `armor_stand` in EMI. The server log should report **0 omitted recipes** (expecting 2,042 on vanilla 26.3 if no datapack changes) and the client should receive the same total. Compare against Fabric and NeoForge; never infer correctness from EMI's total baked recipe count alone.

**Critical Fabric test:** Join, exit, rejoin and `/reload` on an integrated server. Confirm one complete EMI reload per synchronization cycle, not two overlapping reloads. Test reconnect to a remote Fabric server.

**Critical NeoForge test:** Ensure ordinary inventory GUI alignment remains correct; check recipes requiring item transformations and potion ingredient predicates.

**Multiplayer:** Repeat with an actual dedicated server and multiple clients; the supplied v25 logs only verified integrated singleplayer worlds. Test datapack reload, server reconnect, full/logged packet sync and new item recipes.

## Known limitations not silently claimed fixed

- Dynamic cooking fuel values, conditional composting probabilities and custom datapack-defined tool/block transformations still need dedicated gameplay-context support.
- The 26.3 non-batched GUI path should be profiled in large modpacks.
- `minecraft:end_of_frame` shader warnings and Java `Unsafe`/native-access warnings are upstream/runtime warnings present in all loaders and are not changed in this patch.
- JEI deprecation warnings remain build-time warnings.

This is v26 **release-candidate source**, not a validated public release.

</details>

<details>
<summary>PF_EMI_26.3_RECIPE_RUNTIME_FIX_v25.md</summary>

# PF-EMI Minecraft 26.3 — Runtime Fix v25

Applies over the full 26.3 source after patch v24. Overwrite matching files.

## Fabric — game disconnect when entering a world

The Fabric `fabric:recipe_sync` callback used to run
`client.getConnection().registryAccess().lookupOrThrow(Registries.RECIPE)`.
In 26.3 the client registry access does not contain reloadable `minecraft:recipe`,
causing `Missing registry: minecraft:recipe` and a packet-handling disconnect.
The updated code constructs an independent RecipeMap from the Fabric-provided
`SynchronizedRecipes.recipes()` collection by building a temporary MappedRegistry.

## Forge — EMI stays waiting for recipe data

The Forge chunk receiver had the same invalid registry lookup and also did not
use the recipe objects contained in the received chunks. It now merges all
received RecipeHolder chunks into one RecipeMap using the shared factory.
Adds two small diagnostics to the server send and client receive paths. If
Forge still waits, please send the new full latest.log; the two messages
identify whether the sender or receiver is not executing.

**Caveat:** The Forge serializer currently omits recipes whose STREAM_CODEC
cannot encode empty ingredients (272 of 2042 in the supplied log). The fix
restores the available recipes first; omitted shaped recipes still require a
separate network-codec migration. We do NOT claim fully complete Forge recipe
coverage yet.

## NeoForge — unobvious world interaction exception

`AxeItemAccessor` had become a non-mixin Java compatibility class, but still
lived in `dev.emi.emi.mixin.*` and was prohibited from direct loading by the
Mixin classloader. Moved its functional logic to `EmiToolTransformCompat` in
a normal package and updated VanillaPlugin. The original obsolete helper is
left untouched and unreferenced (no deletion requirement for ZIP overlays).

## Testing

- JDK 25 Gradle and Minecraft 26.3 runtime are not available here.
- Full functional build and in-game behavior must be verified in your environment.
- Test: Fabric world join and populated recipes; Forge world join/index + logs;
  NeoForge world-interaction recipes, creative/normal inventory, recipes.

</details>

<details>
<summary>PF_EMI_26.3_FORGE_BREWING_IMPORT_FIX_v24.md</summary>

# PF-EMI 26.3 — Forge Brewing Import Compile Fix v24

## Problem
With v23, Fabric and NeoForge built successfully. Forge failed due to two imports
of deleted `net.minecraftforge.common.brewing` classes in EmiAgnosForge.java.
Neither class is used by the current `addBrewingRecipesAgnos` implementation.

## Fix
Delete only `BrewingRecipe` and `IBrewingRecipe` imports. Shared `EmiBrewingRecipeDiscovery`
is retained; Fabric, NeoForge, other Forge source, and build files are unchanged.

## Apply
Extract into the PF-EMI (26.3) project root after v19–v23, overwriting matching
files. Run `build-26.3-all.bat`. Test the resulting three JARs separately.

## Validation
ZIP CRC, two-import source delta and path correctness verified. Full build and
runtime verification must be completed on the Java 25 test environment.

</details>

<details>
<summary>PF_EMI_26.3_NEOFORGE_COMPILE_FIX_v23.md</summary>

# PF-EMI 26.3 - NeoForge Render-Layer Compile Fix v23

## Problem
`neoforge:compileJava` failed in `EmiClientNeoForge.java:39` because
`NeoForgeRenderTypes.values()` does not exist in NeoForge 26.3.0.84.

## Fix
Remove the obsolete registration line and its unused imports.
Shared `StackBatcher` uses direct GUI item rendering and reports
`isEnabled() == false`; no render-layer registration is necessary.

## Apply
1. Ensure that 26.3 v19-v22 are already applied to your source tree.
2. Extract this ZIP at the root of `PF-EMI (26.3)`, overwriting the Java file and CHANGELOG.
3. Run `build-26.3-all.bat`.

## Scope and testing
NeoForge client initialization only. The NeoForge v18 GUI alignment code is untouched.
Fabric, Forge, and xplat Java code are unchanged. File contents and ZIP integrity
were checked, but Java 25 compilation and in-game testing were not run here.
The build may expose subsequent Forge errors after NeoForge compiles.

</details>

<details>
<summary>PF_EMI_26.3_ACCESS_WIDENER_FIX_v22.md</summary>

# PF-EMI 26.3 — Access Widener Compile Fix v22

## Applies to
A working PF-EMI 26.3 source tree with v19, v20, and v21 already applied.
This is a patch-only ZIP: extract at the *project root* (`PF-EMI (26.3)`).

## Diagnosis
Shared Java and Fabric Java now compile. `:fabric:validateAccessWidener` fails because
`xplat/src/main/resources/emi.accesswidener` still grants access to the removed
`net/minecraft/world/item/alchemy/PotionBrewing$Mix` class.
The original Forge access transformer also has the same obsolete target.

## Changes
- Delete only the obsolete `accessible class ...PotionBrewing$Mix` rule in `emi.accesswidener`.
- Delete only the obsolete `public ...PotionBrewing$Mix` rule in Forge's access transformer.
- Leave remaining rules for Screen/AbstractWidget as they were.
- Update CHANGELOG.md.

## Testing
Archive passed CRC, config sanity, and delta checks. Cannot run ForgeGradle/Java 25 and Minecraft runtime here.
Run `build-26.3-all.bat`; after its build passes, test the three JARs separately.

## Expected result
The specific access-widener error is gone. Additional build or runtime errors may be exposed afterward.

</details>

<details>
<summary>PF_EMI_26.3_RECIPE_REGISTRY_COMPILE_FIX_v21.md</summary>

# PF-EMI 26.3 Compile Fix v21 — Recipe Registry Migration

**Base:** PF-EMI (26.3) with v19 + v20 installed. Patch-only, project-root-relative paths.

## Issue

The Java 25 build now compiles `:xplat:compileJava`, but `:fabric:compileJava` fails on:

    EmiClientFabric.java:70: Collection<RecipeHolder<?>> cannot be converted to HolderLookup<Recipe<?>>
    EmiAgnosFabric.setReceivedRecipeMap(RecipeMap.create(recipes.recipes()));

Minecraft 26.3 moved recipe data to a reloadable registry. `RecipeMap.create(...)`
now expects `HolderLookup<Recipe<?>>` and builds its entries from `listElements()`.

## Changed files

- `fabric/src/main/java/dev/emi/emi/platform/fabric/EmiClientFabric.java` — create the recipe map from the active connection's `Registries.RECIPE` lookup when Fabric fires its recipe-synchronized event.
- `forge/src/main/java/dev/emi/emi/platform/forge/EmiRecipeSync.java` — proactively migrate the same obsolete `RecipeMap.create(all)` call to the synced registry lookup, while retaining the custom chunk handshake and existing packet validation. This is a follow-up compile prevention; the uploaded log has not reached the Forge build.
- `CHANGELOG.md` — appended the v21 migration note.

## Installation

1. Extract this ZIP into `PF-EMI (26.3)` and overwrite the matching files.
2. Run `build-26.3-all.bat` under JDK 25.
3. If it fails, share the new build log. Test the 26.3 Fabric, NeoForge, and Forge JARs independently if compilation succeeds.

## Limits / cautions

- The change is verified against the documented Minecraft 26.3 method signature, but the complete loader builds and in-game runtime were not executed in this environment.
- NeoForge was not modified; it already receives a `RecipeMap` from its sync event.
- The recipe registry is used as the canonical source for Forge's completed sync. This must be tested on both integrated and multiplayer servers to verify that dynamic recipes are available when the final packet arrives.
- Ignore the 10 JEI deprecation warnings unless they become errors. Preserve previous NeoForge inventory GUI alignment and unrelated v20 code.

## API reference

https://mc-packet-reference.netlify.app/26.x/source/net/minecraft/world/item/crafting/recipemap/
https://fabricmc.net/2026/09/15/263.html

</details>

<details>
<summary>PF_EMI_26.3_COMPILE_FIX_v20.md</summary>

# PF-EMI 26.3 Compile Fix v20

Base: v19 applied to the previously working Minecraft 26.2 source through v18.
This is a patch-only overlay. No Fabric/NeoForge/Forge build has been run in this container.

## Compiler errors addressed (36 as reported)
- Deleted 26.3 `AxeItem`, `HoeItem`, `ShovelItem` types: retired four obsolete mixins, updated both common/Forge Mixin configurations, discovered stripped wood through registry identifiers and added vanilla hoe/shovel transformation compatibility recipes.
- Updated `BonemealBlock` signatures without passing fake nullable world instances; tall flowers are eligible for the standard duplication recipe.
- `GuiGraphicsExtractor.tooltip` calls: add seventh `boolean` argument at five call sites.
- JEI slot context fallback: `ContextMap.EMPTY` instead of the now-private builder constructor.
- `KeyEvent.scancode()` -> `KeyEvent.keycode()` at all nine reported sites.
- `Inventory.placeItemBackInInventory` now uses `Prediction.SERVER_ONLY` on cheat and recipe fill packet handlers.
- Composting is now component based (`DataComponents.COMPOSTABLE`): known vanilla `ResolvableInt` providers map to the standard probability; literal-positive `Constant` values are always successful.

## Known limitations
- New datapack-defined axe/hoe/shovel transformations are not automatically displayed as EMI world-interaction recipes by the compatibility fallback. Vanilla stripping, ordinary hoe tilling, and shovel paths remain present. A future implementation should scan the 26.3 BLOCK_TRANSFORMER registry including custom providers.
- Datapack-defined compostable providers requiring an evaluated loot context are skipped rather than assigned fabricated chances.
- Existing v19 rendering, brewing and cooking fuel limitations still apply.
- This patch is NOT proof that 26.3 now builds or runs. Execute `build-26.3-all.bat` on Java 25 and share the new log if another failure occurs.

## Installation
Extract the ZIP into the ROOT of `PF-EMI (26.3)` after v19, replacing corresponding files, then run `build-26.3-all.bat`. Do not extract into a second nested folder. Keep your known-working 26.2 tree unchanged.

</details>

<details>
<summary>PF_EMI_26.3_PORT_v19.md</summary>

# PF-EMI 26.3 source port, candidate v19

This is a patch-only update from the **working 26.2 v18 source tree**.
Do not apply it to the 26.1.2 version or skip earlier patches. Work on a COPY of
your functional 26.2 source so you can roll back.

## Version targets

- Minecraft Java 26.3; Java 25
- Fabric Loader 0.19.5; Fabric API 0.165.0+26.3
- NeoForge 26.3.0.84 (listed on the official Maven index; 26.3.0.95 is not)
- Forge 66.0.12
- JEI 26.3 Fabric API compile-only 31.14.0.72

## Port work included

- SDL key scancodes replace GLFW key constants, including search, recipe navigation,
  preference shortcuts and modified-key matching. SDL mouse IDs are now 1=left,
  2=middle, 3=right, so default mouse binds were rewritten accordingly.
- Update active Renderpearl imports in screenshot capture and GUI accessors.
- Update the Forge mod loader/version ranges, NeoForge dependencies, Fabric metadata,
  resource-pack metadata, and root/Forge Gradle properties.
- Preserve the v17 resource-reload startup fix and v18 NeoForge screen overlay placement.
- SDL search text input now reports focus changes to Minecraft.
- One 26.3 build script (`build-26.3-all.bat`) calls the existing three-loader script
  **once**. The previous wrapper erroneously attempted Forge twice.

## Windows instructions

1. Copy the complete working `PF-EMI (26.2)` source folder to `PF-EMI (26.3)`.
2. Extract this ZIP INTO THE COPY, replacing matching source files.
3. Run `build-26.3-all.bat` under JDK 25.
4. The `release/` folder should contain `PF-EMI-26.3.jar`,
   `PF-EMI-26.3-NEOFORGE.jar`, and `PF-EMI-26.3-FORGE.jar` if ALL succeeded.
5. Test separately in matching Minecraft 26.3 profiles with NO older PF-EMI JARs.

## Known gaps / cautions

- This is a pre-compile source port, NOT a verified release or a finished gameplay port.
  Minecraft 26.3 also changes brewing recipes, registry reloads, dynamic fuels,
  some GUI/mixin internals, and Renderpearl's GPU APIs. More compilation errors
  and in-game fixes may be needed; report the full build log.
- Source has the existing v13 non-batched GUI renderer; large item lists may have
  different performance characteristics.
- Old EMI keybind configurations saved from GLFW-era versions contain old numeric
  key IDs. Back up and regenerate EMI configuration when testing if keybinds fail.
- Test normal inventories on NeoForge to ensure the v18 sidebars stay outside the
  inventory, then test Fabric/Forge, recipes, search, screenshots, and joining a world.
- No Minecraft runtime build can be performed in this environment (JDK 25 and
  cached loader artifacts unavailable). This patch is structurally checked only.

References: https://fabricmc.net/2026/09/15/263.html ;
https://docs.neoforged.net/primer/docs/26.3/

## 26.3 datapack brewing support

Minecraft 26.3 removed `PotionBrewing`. v19 also replaces all three loaders' legacy brewing registry access with a shared `RecipeType.BREWING` reader. The reader walks synced `RecipeMap` entries, generates candidates from ingredient and potion predicates, and adds EMI brewing recipes for matching items. It does not yet represent potion-specific reagent predicates and should be tested with datapack/modded brewing recipes. The retired Mixin accessor has been removed from the Fabric and Forge Mixin lists.

## 26.3 furnace-fuel migration

`FuelValues` no longer exists; the new shared `EmiFuelDiscovery` reads `DataComponents.COOKING_FUEL` for all registered items. Constant `burnTime` values are exact. Registry-reference dynamic burn-time providers require a server-side loot context, so this initial client-side port uses representative vanilla values for coal, charcoal, coal block, blaze rod, lava bucket, dried kelp block and bamboo, and omits other unknown dynamic fuel times rather than displaying a fabricated number. Full dynamic fuel support remains a follow-up task.

</details>


### 26.2 original notes

<details>
<summary>PF_EMI_26.2_NEOFORGE_GUI_ALIGNMENT_FIX_v18.md</summary>

# PF-EMI 26.2 – NeoForge Inventory GUI Position Fix v18

## Scope
NeoForge-only. Apply after v17. Only the NeoForge client rendering handler is modified; shared, Fabric, and Forge sources remain unchanged.

## Cause
NeoForge 26.2 posts `ScreenEvent.Render.Foreground` after `AbstractContainerScreen.extractContents()` completes. The inventory's `GuiGraphicsExtractor` pose has already been restored from its left/top origin translation at that point. The earlier port retained a compensating negative `leftPos`/`topPos` translation from the removed `ContainerScreenEvent.Render.Foreground` rendering stage, shifting EMI's sidebars across the inventory, above and left of their calculated positions.

## Change
Remove the negative pose translation from the NeoForge foreground callback. Preserve the push/pop, event filter, mouse coordinates, and render order. EMI sidebars use screen-space positions, so no additional offset is required.

## Installation
1. Extract this ZIP at the root of your existing `PF-EMI (26.2)` project (with v17 already applied).
2. Overwrite the matching Java file.
3. Run `build-26.2-all.bat`.
4. Replace only the NeoForge JAR in your NeoForge 26.2 Modrinth profile.
5. Test player inventory and chest/creative inventory. Compare against Fabric and Forge for consistent sidebar positions and tooltips.

## Validation
Verified source/ZIP structure and code against the 26.2 NeoForge `ScreenEvent.Render.Foreground` injection timing. NOT compiled or tested in Minecraft in this environment.

</details>

<details>
<summary>PF_EMI_26.2_STARTUP_FIX_v17.md</summary>

# PF-EMI Minecraft 26.2 — three-loader startup fix v17

**Apply over v16.** This is a patch-only archive rooted at the PF-EMI (26.2) project directory. It is **not** a compiled mod jar.

## Evidence from the three runtime failures

- **Fabric:** `emi.mixins.json:MinecraftClientMixin` fails at `reloadResourcePacks(ZLnet/minecraft/client/Minecraft$GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;`. The hard-coded old descriptor matches no method in the tested 26.2 runtime.
- **NeoForge:** the screenshot shows the same `MinecraftClientMixin.reloadResources` mixin targeting failure in `emi.mixins.json`. Shared source correction covers both loaders.
- **Forge:** crash report says mod EMI requires `forge [64.1.5,65)` and `minecraft [26.1.2,26.2)` despite running Forge `65.1.5` and Minecraft `26.2`. The Forge metadata was still from the previous Minecraft release.

## Source corrections

1. `xplat/src/main/java/dev/emi/emi/mixin/MinecraftClientMixin.java`: make the resource-reload hook match by method name, removing the stale parameter descriptor; use `require = 0` to avoid blocking startup if the internal Minecraft method is renamed again. Keep the EMI recipe-map/client-world guard and schedule reload work on the Minecraft client thread instead of allocating a new thread pool on each reload. Existing loader-specific resource listeners and recipe-sync hooks are unchanged.
2. `forge/src/main/resources/META-INF/mods.toml`: update Forge loader requirement to 65, Forge dependency to `[65.1.5,66)` and Minecraft dependency to `[26.2,26.3)`; update the port description.

## Apply and test

1. Extract the ZIP into `PF-EMI (26.2)`, overwriting matching paths.
2. Run `build-26.2-all.bat` using Java 25.
3. Remove outdated PF-EMI JARs from each test profile before copying the new matching release JAR. The Forge crash report specifically loaded `mods/1.0+26.2+forge.jar`, **not** a file named `PF-EMI-26.2-FORGE.jar`, so ensure you replace the actual JAR being loaded.
4. Launch each loader separately. Check inventory/reload behavior as well as startup.
5. If any loader fails, share its *new* latest.log/crash report. A successful build is not a runtime test.

## Validation and caveats

- ZIP layout and source string substitutions are checked; a full Java 25 Gradle compile and Minecraft runtime test are not available here.
- The reload hook may be skipped on Minecraft versions where `reloadResourcePacks` does not exist. Existing platform reload listeners still run, but the optional EMI refresh on client resource-pack changes may need a later loader-specific implementation.
- This update intentionally makes no modifications to previously compiling Fabric/NeoForge entrypoints or rendering. Other 26.2 runtime Mixin mismatches may surface later.

</details>

<details>
<summary>PF_EMI_26.2_FORGE_COMPILE_FIX_v16.md</summary>

# PF-EMI 26.2 — Forge overlay compile fix v16

## Prerequisites
Apply v12 through v15 before this patch.

## Changed files
- `forge/src/main/java/dev/emi/emi/platform/forge/EmiClientForge.java`
  - Replace `Minecraft.getInstance().getOverlay()` (removed in 26.2) with `Minecraft.getInstance().gui.overlay()`.
  - This matches the 26.2 accessor already used by the shared `KeyboardMixin.java` and preserves the guard against character input while a GUI overlay is open.

## Build
Extract this patch to the `PF-EMI (26.2)` project root and overwrite existing files.
Run `build-26.2-all.bat` to rebuild Fabric, NeoForge, then Forge.

## Status
Addresses the single Forge Java compiler error shown in the 2026-10-27 log. No Java 25 build or runtime testing was performed in this environment; further compilation/runtime issues may appear.

</details>

<details>
<summary>PF_EMI_26.2_NEOFORGE_COMPILE_FIX_v15.md</summary>

# PF-EMI 26.2 — NeoForge foreground event compile fix v15

## Applies to
- Minecraft 26.2, NeoForge 26.2.0.93.
- Install over PF-EMI 26.2 with port patches v12–v14 already applied.

## Fix
- `ContainerScreenEvent.Render.Foreground` was removed in NeoForge 26.2.
- Replace its event listener parameter with `ScreenEvent.Render.Foreground`.
- Inspect `event.getScreen()` and process only `AbstractContainerScreen<?>` so normal screens are ignored.
- Preserve the existing overlay render ordering, matrix offsets, tooltips and other behavior.

## Installation
Extract the ZIP to the project root, overwriting only the affected NeoForge Java file; then run `build-26.2-all.bat`.

## Test status
Confirmed the replacement event and its getter contract against NeoForge 26.2.x source; checked Java source structure and ZIP integrity. No local Java 25 build or runtime test was possible. Fabric/Xplat source and Forge source were not modified. Further loader-specific errors may appear after compilation advances.

</details>

<details>
<summary>PF_EMI_26.2_COMPILE_FIX_v14.md</summary>

# PF-EMI Minecraft 26.2 - compile errors v14

Patch only; apply **after v13**. Extract this ZIP at the project root, preserving paths, then run `build-26.2-all.bat`.

## Fixes from `Pasted text(20261027-060156).txt`

- `JemiRecipeSlot`: implement JEI 30 `getDisplayedIngredients()`; handle empty slots without indexing an empty list.
- `JemiRecipeSlotBuilder`: implement JEI 30 overload `setFluidRenderer(..., TilingDirection)` and `setHoverPadding(...)`. The new tiling/padding hints currently defer to the old EMI renderer/slot, so these JEI 30 features are not yet fully implemented.
- `AbstractInventoryScreenMixin`: fix the `Hud` Java package to `net.minecraft.client.gui.Hud`, as specified by Minecraft 26.2's GUI reorganization.
- `VanillaPlugin`: resolve colored concrete/concrete-powder blocks through vanilla registry identifiers, retaining all 16 world-interaction recipes and avoiding removed static `Blocks.*_CONCRETE` fields.

## Verification and limitations

- Compared the supplied v13 source against **all 35 errors** in the uploaded Java build log.
- Verified the new JEI method signatures against the 26.2 `mezz/JustEnoughItems` source.
- Verified the `Hud` package and method using the official NeoForge 26.2 migration guide / published 26.x documentation.
- Static source checks and ZIP integrity pass; a complete JDK 25/Gradle build is **not available** in this environment. Further loader-specific errors or runtime problems may appear once `xplat` compiles.
- Fabric/NeoForge/Forge loader code is not changed by v14.

</details>

<details>
<summary>PF_EMI_26.2_COMPILE_MIGRATION_v13.md</summary>

# PF-EMI Minecraft 26.2 - source compile migration v13

Applies **after v12**, using the same patch-root layout. Rebuild with `build-26.2-all.bat`.

## Shared Java compile updates
- Migrated the removed old screen search Tuple helper to an equivalent local class.
- Replaced the removed `I18n.exists` calls with `Language#getInstance().has`.
- Migrated the old static colored banner item references to registry IDs (16 vanilla colors).
- Replaced removed `ChatFormatting#getColor` references with the corresponding opaque GUI colors.
- Removed unused `Tesselator` reference and moved effect sprites into `Hud`.
- Updated the screenshot target format, clear color, mapped GPU buffers, pixel stride, and chat routing.
- Adapted the JEI 30 widget bridge to the 26.2 additional builder/tooltip interfaces.

## Rendering caveat
Minecraft 26.2 removed `MultiBufferSource` and direct `RenderType#draw`. This candidate
intentionally disables **cached stack batching** and draws ingredient icons directly
through `GuiGraphicsExtractor`. Rendering remains functional but may be slower with
very large sidebars. The old GPU batching mechanism must be rewritten using the
new feature submission pipeline before it can safely be reenabled.

## Verification
- Source was inspected and patch contents checked for old symbol references.
- Full Minecraft 26.2 compile or Forge/Fabric/NeoForge runtime tests were **not available** in this environment (only Java 21 installed). This is a follow-up **candidate**, not a verified 3-loader release.
- If new compiler errors remain, send the build output. An additional 26.2 API iteration is expected.

</details>

<details>
<summary>PF_EMI_26.2_PORT_CANDIDATE_v12.md</summary>

# PF-EMI Minecraft 26.2 — port candidate v12

This is a **patch-only** archive based on the 26.1.2 v11 source state. Extract into the existing project root and overwrite files.

## Confirmed dependencies

- Minecraft 26.2; Java 25+.
- Fabric Loader 0.19.5 and Fabric API 0.161.0+26.2.
- NeoForge 26.2.0.93 (confirmed Maven release).
- Forge 26.2-65.1.5 (official MDK listed).
- JEI Fabric API/implementation 30.48.0.242 (Maven published 2026-10-25); compile-only integration.
- Mod Menu optional integration: existing source may need a 26.2 API bump.

## Changes

- Updated root and standalone Forge Gradle versions.
- Aligned Fabric Mod Menu optional dependency with v20.0.3 and removed stale 26.1 runtime-only mod dependencies.
- Updated supported Minecraft 26.2 ranges in Fabric/NeoForge/Forge mod metadata.
- Updated and locked 26.2 profile, including Fabric Loader and JEI.
- Migrated Minecraft GUI screen access from `client.screen` to `client.gui.screen()` and `setScreen` to `client.gui.setScreen` in shared Java and applicable loaders.
- Corrected pack metadata to 26.2 resource format 88 when available.
- Re-targeted existing 26.x builders and added `build-26.2-all.bat` (sequential three-loader build).
- Preserved prior Forge recipe serialization and native input fixes by touching only the needed files.

## Build

Run `build-26.2-all.bat` from the project root after applying v11. Build outputs belong in `release/`. The Forge JAR validator still checks compatibility and input configuration.

## Limitations

- **Not compiled or tested against Minecraft 26.2:** execution environment only has JDK 21 and no cached 26.2 game/loader libraries. The 26.2 migration includes additional rendering changes (especially `Font`), potential ChatFormatting removal, and other method signature changes. Compiler errors and gameplay bugs may remain.
- This is a source port candidate, NOT playable JARs. Do not install the ZIP in `mods/`.
- The prior 26.1.2 Forge custom recipe synchronization had an unverified encode failure during world join; v11 safeguards are retained but are not proven to work on 26.2.
- 26.2 can use Vulkan; raw OpenGL rendering paths require separate testing.

## Validation & references

- https://fabricmc.net/2026/06/15/262.html
- https://docs.architectury.dev/api/26.2.x/getting-started/setup/
- https://github.com/neoforged/.github/blob/main/primers/26.2/index.md
- https://files.minecraftforge.net/net/minecraftforge/forge/index_26.2.html

## Source edits

- Java files rewritten: 20
- Total patch paths: 39

</details>

<details>
<summary>FORGE_BUILD_LOG_26.2.txt</summary>

WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::load has been called by net.rubygrapefruit.platform.internal.NativeLibraryLoader in an unnamed module (file:/C:/Users/themo/.gradle/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/lib/native-platform-0.22-milestone-29.jar)
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
WARNING: Restricted methods will be blocked in a future release unless native access is enabled


> Configure project :
Minecraft Mavenizer 0.5.21 by Forge Development LLC
  Output:             C:\Users\themo\Downloads\PF-EMI\PF-EMI (26.2)\forge\.gradle\mavenizer\repo
  Output JSON:        C:\Users\themo\Downloads\PF-EMI\PF-EMI (26.2)\forge\.gradle\mavenizer\dependencies\default.json
  Dependencies Only:  false
  Cache:              C:\Users\themo\.gradle\caches\minecraftforge\forgegradle\mavenizer\caches
  JDK Cache:          C:\Users\themo\.gradle\caches\minecraftforge\forgegradle\mavenizer\caches
  Offline:            false
  Cache Only:         false
  Ignore Cache:       false
  Mappings:           official-26.2
  Foreign Repos:      [https://maven.fabricmc.net/, https://repo.maven.apache.org/maven2/, https://maven.blamejared.com/, https://maven.minecraftforge.net/, https://libraries.minecraft.net/]
  GradleVariantHack:  true
  Disable Gradle:     false
  Stub Jars:          false
  Access Transformer: C:\Users\themo\Downloads\PF-EMI\PF-EMI (26.2)\forge\src\main\resources\META-INF\accesstransformer.cfg

Processing Minecraft dependency: net.minecraftforge:forge:26.2-65.1.5
Processing Minecraft Forge (userdev): 26.2-65.1.5
Mappings Zip
  srg2names[official-26.2][Empty]
Mappings POM
  pom[mappings_official]
Client Extra
  downloadLauncherManifest
  downloadVersionJson[26.2]
  download[26.2][client]
  extra[client]
  access-transform
    Cache miss!
    java.lang.Exception: Cache miss! Stacktrace for Information Only
    	at net.minecraftforge.mcmaven.impl.Mavenizer.assertNotCacheOnly(Mavenizer.java:60)
    	at net.minecraftforge.mcmaven.impl.Mavenizer.checkCache(Mavenizer.java:94)
    	at net.minecraftforge.mcmaven.impl.MinecraftMaven.accessTransform(MinecraftMaven.java:515)
    	at net.minecraftforge.mcmaven.impl.MinecraftMaven.lambda$accessTransform$0(MinecraftMaven.java:502)
    	at net.minecraftforge.mcmaven.impl.util.Task$Simple.execute(Task.java:145)
    	at net.minecraftforge.mcmaven.impl.repo.Repo$PendingArtifact.get(Repo.java:243)
    	at net.minecraftforge.mcmaven.impl.MinecraftMaven.finalize(MinecraftMaven.java:408)
    	at net.minecraftforge.mcmaven.impl.MinecraftMaven.createForge(MinecraftMaven.java:241)
    	at net.minecraftforge.mcmaven.impl.MinecraftMaven.run(MinecraftMaven.java:119)
    	at net.minecraftforge.mcmaven.cli.MavenTask.run(MavenTask.java:218)
    	at net.minecraftforge.mcmaven.cli.Main.run(Main.java:70)
    	at net.minecraftforge.mcmaven.cli.Main.main(Main.java:23)
Client Maven POM
  pom[client-extra]
Sources
  stripClient
  download[26.2][server]
  extractServer
  stripServer
  merge
  modifyAccess
  stripSides
  listLibraries
  decompile
  patch
  patch[forge]
  downloadSources
  injectSources[forge]
  remap[forge][official-26.2][javadoc]
  remap[forge][official-26.2][javadoc][variants]
Classes
  recompile[forge][official-26.2]
  injectData[forge][official-26.2]
  access-transform
  injectData[forge][official-26.2][variants]
Maven POM
  pom[forge]
Metadata
  metadata[forge]
  metadata[forge][variants]
Minecraft Maven has finished, took 0:06.772

> Task :clean
> Task :compileJava FROM-CACHE
> Task :processResources
> Task :classes
> Task :jar
> Task :assemble
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :check UP-TO-DATE
> Task :build

[Incubating] Problems report is available at: file:///C:/Users/themo/Downloads/PF-EMI/PF-EMI%20(26.2)/forge/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.6.0/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 9s
4 actionable tasks: 3 executed, 1 from cache

</details>


### 26.1 / Forge original notes

<details>
<summary>FORGE_60.1.9_FIX_V25.md</summary>

# PF-EMI 1.21.10 Forge 60.1.9 compatibility fix — v25

The 1.21.10 Forge build was rejected before PF-EMI initialized because `mods.toml` required Forge 60.1.15+, while the test profile uses Forge 60.1.9.

Changes:
- `forge_version=1.21.10-60.1.9`
- Forge runtime range: `[60.1.9,61)`
- `APPLY_PORT.bat` validation updated to the 60.1.9 baseline
- release/readme notes updated accordingly

No Fabric, NeoForge, recipe, GUI, or gameplay logic changed.

</details>

<details>
<summary>FORGE_26.1.2_RECIPE_SYNC_PACKET_FIX_v11.md</summary>

# PF-EMI Forge 26.1.2: recipe sync packet guard (v11)

## Observed symptom

On entering a world, Minecraft reports `Failed to encode packet clientbound/minecraft:custom_payload`. The attached `latest.log` covers startup but does not include a server-side encoder exception or a stack trace identifying the precise payload. The Forge EMI recipe-sync channel is a likely candidate, **not confirmed yet**.

## Changes

- Preflight every recipe with `RecipeHolder.STREAM_CODEC` in a registry-aware buffer before sending EMI's recipe chunk packet.
- Omit individually non-serializable recipes (including possible Minecraft 26.1 unit-codec singleton mismatches), log the recipe IDs, and sync remaining recipes instead of disconnecting the player.
- Bound both entry count (16) and encoded chunk size (96 KiB) to avoid oversized custom payloads.
- Include recipe IDs if network-thread encoding still throws.
- Correct Forge `pack.mcmeta` to use mandatory `min_format` and `max_format` for MC 26.1.2 resource format 84.

## Limitations

Skipped recipes will not appear in the EMI client recipe map. The root cause cannot be confirmed until a server-side log from the actual failed world join identifies the payload/exception. This fix does not change Fabric or NeoForge. Minecraft runtime/Java 25 compilation not available for testing in this environment.

## Applying

1. Extract this patch ZIP at the PF-EMI (26.1.2) project root (not inside `forge/`).
2. Run `build-forge-26x.bat`.
3. Choose to replace the Modrinth JAR when prompted, or replace `PF-EMI-26.1.2-FORGE.jar` manually.
4. Test a new single-player world. If it disconnects, upload BOTH latest.log and the integrated server log (or full crash report) from this new attempt, plus the Forge build log.

</details>

<details>
<summary>FORGE_26.1.2_INPUT_RUNTIME_FIX_v10.md</summary>

# PF-EMI 26.1.2 Forge native GUI input fix v10

## Cause (from the new 23:39:44 crash log)

Forge now accepts `JAVA_21` in `emi-forge26.mixins.json` and starts Minecraft.
The next fatal error is `emi-forge26.mixins.json:MouseMixin`:
`Critical injection failure: onMouseDown(...) (0/1) succeeded. Scanned 0 target(s). No refMap loaded.`
This is distinct from the v8/v9 `JAVA_25` error. The Forge JVM and JAR metadata are working.

## Files changed (patch-only)

- `forge/src/main/resources/emi-forge26.mixins.json`: drops `MouseMixin` and `KeyboardMixin` **only in the Forge configuration**, retains all other EMI mixins and `JAVA_21`.
- `forge/src/main/java/dev/emi/emi/platform/forge/EmiClientForge.java`: routes GUI press/release/drag/scroll/key/character events using Forge 26.1.2's native ScreenEvent.Pre event buses, following EventBus 7's boolean cancellation API. Retains pressed-button modifiers for release and drag.
- `verify-forge26-jar.ps1`: rejects a source configuration or built JAR which still registers the removed input mixins. The unchanged v9 `build-forge-26x.bat` automatically calls this validator and deploys the verified build after confirmation.

## Installation

1. Extract this ZIP over the **root** of your `PF-EMI (26.1.2)` source tree; overwrite the three named files.
2. Run `build-forge-26x.bat` from v9. Requires JDK 25. Do not use the ordinary Fabric/NeoForge batch.
3. On success, confirm deployment to the Forge Modrinth profile, or manually replace the JAR with `release/PF-EMI-26.1.2-FORGE.jar`.
4. Test a world, open inventory, hover items, click/sidebar/search, type, drag and scroll. If startup fails, upload the **new** complete launch log; if build fails, upload `FORGE_BUILD_LOG_26.1.2.txt`.

### Limitations

This source patch was checked against Forge 26.1 source event definitions and the current EMI interfaces, but the actual Java 25 Forge build and Minecraft runtime could not be executed in this environment. Other shared mixins (not handled by this patch) may expose additional runtime incompatibilities. Fabric and NeoForge source/build files are untouched.

</details>

<details>
<summary>FORGE_26.1.2_RUNTIME_FIX_v9.md</summary>

# PF-EMI 26.1.2 Forge runtime/config guard v9

The newest uploaded launch log is **byte-for-byte identical** to the previous one: timestamp `23:14:47` and error `JAVA_25 is not recognised`. This is the *same crash*, not a different error following v8.

## What the patch does

* Re-includes the corrected `forge/src/main/resources/emi-forge26.mixins.json` (`compatibilityLevel: JAVA_21`).
* Adds a PowerShell validator that checks source JSON and **actual built JAR archive** (Mixin JSON, entry point class, Forge metadata, manifest) and prints SHA-256.
* Replaces `build-forge-26x.bat` to run a clean Forge-only build and refuse to release stale `JAVA_25` artifacts.
* After a successful verified build, **asks for confirmation** before deploying into the Modrinth profile named `Forge 1.21.7`, the profile found in the uploaded crash log. Backs up the old JAR as `.jar.bak` before replacing it.
* Leaves Fabric and NeoForge build scripts and sources untouched.

## Apply

1. Extract this patch at the root of `PF-EMI (26.1.2)` and overwrite existing files.
2. Run `build-forge-26x.bat` (not the main Fabric/NeoForge batch).
3. On a verified build, press `Y` to install to the profile from your crash log, if that folder exists. Otherwise copy `release/PF-EMI-26.1.2-FORGE.jar` into the current Forge instance mods folder.
4. Launch Minecraft again and send the *new* log if it fails. For build errors, send `FORGE_BUILD_LOG_26.1.2.txt`.

Do **not** copy this patch ZIP itself into the Minecraft mods directory. It contains source/build files.

### Important

A successful Gradle build or v9 validation does not prove all mixins or game integrations work at runtime; it specifically prevents release of the known wrong `JAVA_25` config. Requires Java 25 for the game build and Windows PowerShell for validation.

</details>

<details>
<summary>FORGE_26.1.2_RUNTIME_FIX_v8.md</summary>

# PF-EMI Minecraft 26.1.2 Forge — Runtime Mixin Fix v8

## Root cause
The uploaded 2026-10-27 Modrinth Forge startup log reports:
`MixinInitialisationError: ... emi-forge26.mixins.json specifies compatibility level JAVA_25 which is not recognised`

Minecraft Forge 64.1.5 loads the upstream SpongePowered Mixin 0.8.7 runtime rather than Fabric Loader's newer fork; that runtime does not recognize the `JAVA_25` compatibility-level token.

## Changes
- Set `forge/src/main/resources/emi-forge26.mixins.json` compatibilityLevel from `JAVA_25` to `JAVA_21`, which the runtime accepts.
- Keep Forge Java 25 compilation and all other source, resource and build files unchanged.
- No Fabric or NeoForge changes.

## Apply
1. Extract this ZIP over the existing PF-EMI (26.1.2) project root and overwrite the file.
2. Run `build-forge-26x.bat` to create a **new** `release/PF-EMI-26.1.2-FORGE.jar`.
3. Replace the old Forge JAR in the Modrinth profile with the newly rebuilt one, and launch Minecraft 26.1.2 Forge 64.1.5.

The pre-v8 JAR still contains `JAVA_25` and will still crash if reused.

## Validation
The JSON syntax was validated and the ZIP integrity checked. Minecraft startup with this patch has not been verified; other runtime mixin issues may be revealed once initialization gets past this configuration check.

</details>

<details>
<summary>FORGE_26.1.2_COMPILE_FIX_v7.md</summary>

# PF-EMI 26.1.2 — Forge build fix v7

The `problems-report(5).html` Gradle `compileJava` task found two compiler errors at
`forge/src/main/java/dev/emi/emi/platform/forge/EmiClientForge.java:53`:

- `getLeftPos()` is not a method of `AbstractContainerScreen<?>`.
- `getTopPos()` is not a method of `AbstractContainerScreen<?>`.

The existing common Mixin accessor `dev.emi.emi.mixin.accessor.HandledScreenAccessor`
provides `getX()` and `getY()` for the `leftPos`/`topPos` fields. Forge's
`emi-forge26.mixins.json` already registers that accessor. This patch uses it
in the screen render translation.

## Apply
Unzip this archive directly into your existing `PF-EMI (26.1.2)` project
folder (overwrite existing files), then run `build-forge-26x.bat`.

Only Forge's `EmiClientForge.java` is modified; Fabric, NeoForge, and
shared xplat files remain untouched.

## Status
The ZIP and source edits were checked locally against the uploaded source
and mixin registration. A Java 25 ForgeGradle compilation and an in-game
launch have not been performed here.

</details>

<details>
<summary>FORGE_26.1.2_BUILD_FIX_v6.md</summary>

# PF-EMI / Forge 26.1.2 — compilation fix v6

## What the supplied problems-report(4).html proves

The standalone `forge/` build reached `:compileJava`, but javac stopped with 31 errors and 54 warnings. The report has two concrete error groups:

- The Sponge Mixin annotation processor tries to remap Minecraft member names using old `searge/notch` obfuscation mappings. Minecraft 26.1.2 is already unobfuscated. Those refmap lookups cannot succeed and produce many `Unable to locate obfuscation mapping` errors for `xplat` mixins.
- `xplat/.../EmiStackConvertible.java` imports the Fabric loader's `@Environment` and `EnvType` annotations, which are not on the Forge-only Java compilation classpath.

## Patch

The **only changed executable file** is `forge/build.gradle`:

1. Remove the obsolete `org.spongepowered:mixin:0.8.7:processor` annotation processor. Preserve the Forge EventBus validator. The actual Mixin classes and the Forge Mixin manifest remain unchanged.
2. Add the Fabric Maven repository solely to resolve `net.fabricmc:fabric-loader:0.19.2` as a **non-transitive compileOnly** dependency. It satisfies the two API annotations without packaging Fabric Loader or making Forge a Fabric mod.

Your working Fabric, NeoForge and shared `xplat` source code stay unchanged. The Forge networking/recipe-sync changes from v4 remain in place.

## Install

Extract the ZIP at the root of the existing `PF-EMI (26.1.2)` folder, merge directories, and replace `forge/build.gradle`. Then run `build-forge-26x.bat` from v5. It saves its error transcript as `FORGE_BUILD_LOG_26.1.2.txt` if compilation still fails.

## Verification limits

- Static file checks passed.
- A real ForgeGradle compile **was not executed** here because this runtime has Java 21, not the Java 25 required by Minecraft 26.1.2, and remote Gradle dependencies are unavailable.
- A successful compile is not proof the standalone Forge mod will run in game: Forge networking, Mixin loading and full recipe sync still require client and dedicated-server testing.

</details>

<details>
<summary>FORGE_26.1.2_PORT_NOTES.md</summary>

# PF-EMI Forge 26.1.2 port (first build candidate)

The existing Fabric and NeoForge sources are not touched. The legacy `forge` module
has been replaced with an independent ForgeGradle 7 (Java 25 / Forge 64.1.5)
project. It compiles the **same Mojang-named `xplat` Java sources** as the working
Fabric and NeoForge builds, excluding 12 orphaned pre-26.x classes, and adds
native Forge 26.1.2 adapters.

Build Forge only (Windows): `build-forge-26x.bat` from the project root.
This does not rebuild your already-working Fabric or NeoForge versions.

To build all three versions instead, use `build-26x.bat` from the project root. The Forge build
runs after Fabric and NeoForge and attempts to create
`release/PF-EMI-26.1.2-FORGE.jar`.

Build Forge only for troubleshooting: `cd forge && gradlew.bat clean build`.

## Work performed
- Ported Mod entrypoint, EventBus 7 registration, config screen, GUI render hooks
- Rewrote old Forge network channel handlers for official-name
  `RegistryFriendlyByteBuf` and current ChannelBuilder
- Added chunked full-recipe synchronization, using RecipeHolder.STREAM_CODEC
  instead of NeoForge-only RecipesReceivedEvent
- Adapted ingredient/fluid handling and tooltip rendering to Forge 26.1.2
- Added native access transformer and a Forge-only Mixin manifest (no generated GlobalMixin)
- Enabled independent ForgeGradle 7 / Java 25 build, official mapping
- Updated Windows and Linux release scripts to collect Forge output

## Validation / remaining concerns
- **Not compiled here**: Java 25 and dependency downloads are unavailable in
  the execution environment. Code is a first pass and may require API fixes.
- **Not in-game tested**. Do not publish a Forge release until loading, mod menu,
  full recipes, JEI compatibility, screen hover tooltips, recipes on dedicated
  servers, and recipe resync after /reload are tested.
- Forge FluidStack remains legacy-NBT in 26.1.2. DataComponentPatch is currently
  empty in Forge-to-EMI conversion; custom Forge fluid-stack NBT is not preserved.
- Full-recipe synchronization is chunked by number of recipes (16 per packet),
  not exact serialized byte size. Extremely large modded recipes may require
  smaller chunks or byte-bound packet batching.
- If Forge compilation fails, capture the **console output** or `--stacktrace`
  log. Gradle problems-report.html alone does not always contain the first fatal
  error, especially dependency-resolution errors.

</details>

<details>
<summary>FORGE_BUILD_LOG_26.1.2.txt</summary>

WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::load has been called by net.rubygrapefruit.platform.internal.NativeLibraryLoader in an unnamed module (file:/C:/Users/themo/.gradle/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/lib/native-platform-0.22-milestone-29.jar)
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
WARNING: Restricted methods will be blocked in a future release unless native access is enabled


> Configure project :
Minecraft Mavenizer 0.5.21 by Forge Development LLC
  Output:             C:\Users\themo\Downloads\PF-EMI\PF-EMI (26.1.2)\forge\.gradle\mavenizer\repo
  Output JSON:        C:\Users\themo\Downloads\PF-EMI\PF-EMI (26.1.2)\forge\.gradle\mavenizer\dependencies\default.json
  Dependencies Only:  false
  Cache:              C:\Users\themo\.gradle\caches\minecraftforge\forgegradle\mavenizer\caches
  JDK Cache:          C:\Users\themo\.gradle\caches\minecraftforge\forgegradle\mavenizer\caches
  Offline:            false
  Cache Only:         false
  Ignore Cache:       false
  Mappings:           official-26.1.2
  Foreign Repos:      [https://maven.fabricmc.net/, https://repo.maven.apache.org/maven2/, https://maven.blamejared.com/, https://maven.minecraftforge.net/, https://libraries.minecraft.net/]
  GradleVariantHack:  true
  Disable Gradle:     false
  Stub Jars:          false
  Access Transformer: C:\Users\themo\Downloads\PF-EMI\PF-EMI (26.1.2)\forge\src\main\resources\META-INF\accesstransformer.cfg

Processing Minecraft dependency: net.minecraftforge:forge:26.1.2-64.1.5
Processing Minecraft Forge (userdev): 26.1.2-64.1.5
Mappings Zip
  srg2names[official-26.1.2][Empty]
Mappings POM
  pom[mappings_official]
Client Extra
  downloadLauncherManifest
  downloadVersionJson[26.1.2]
  download[26.1.2][client]
  extra[client]
  access-transform
    Cache miss!
    java.lang.Exception: Cache miss! Stacktrace for Information Only
    	at net.minecraftforge.mcmaven.impl.Mavenizer.assertNotCacheOnly(Mavenizer.java:60)
    	at net.minecraftforge.mcmaven.impl.Mavenizer.checkCache(Mavenizer.java:94)
    	at net.minecraftforge.mcmaven.impl.MinecraftMaven.accessTransform(MinecraftMaven.java:515)
    	at net.minecraftforge.mcmaven.impl.MinecraftMaven.lambda$accessTransform$0(MinecraftMaven.java:502)
    	at net.minecraftforge.mcmaven.impl.util.Task$Simple.execute(Task.java:145)
    	at net.minecraftforge.mcmaven.impl.repo.Repo$PendingArtifact.get(Repo.java:243)
    	at net.minecraftforge.mcmaven.impl.MinecraftMaven.finalize(MinecraftMaven.java:408)
    	at net.minecraftforge.mcmaven.impl.MinecraftMaven.createForge(MinecraftMaven.java:241)
    	at net.minecraftforge.mcmaven.impl.MinecraftMaven.run(MinecraftMaven.java:119)
    	at net.minecraftforge.mcmaven.cli.MavenTask.run(MavenTask.java:218)
    	at net.minecraftforge.mcmaven.cli.Main.run(Main.java:70)
    	at net.minecraftforge.mcmaven.cli.Main.main(Main.java:23)
Client Maven POM
  pom[client-extra]
Sources
  stripClient
  download[26.1.2][server]
  extractServer
  stripServer
  merge
  modifyAccess
  stripSides
  listLibraries
  decompile
  patch
  patch[forge]
  downloadSources
  injectSources[forge]
  remap[forge][official-26.1.2][javadoc]
  remap[forge][official-26.1.2][javadoc][variants]
Classes
  recompile[forge][official-26.1.2]
  injectData[forge][official-26.1.2]
  access-transform
  injectData[forge][official-26.1.2][variants]
Maven POM
  pom[forge]
Metadata
  metadata[forge]
  metadata[forge][variants]
Minecraft Maven has finished, took 0:06.063

> Task :clean
> Task :compileJava FROM-CACHE
> Task :processResources
> Task :classes
> Task :jar
> Task :assemble
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :check UP-TO-DATE
> Task :build

[Incubating] Problems report is available at: file:///C:/Users/themo/Downloads/PF-EMI/PF-EMI%20(26.1.2)/forge/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.6.0/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 8s
4 actionable tasks: 3 executed, 1 from cache

</details>


### Minecraft 1.21.x original notes

<details>
<summary>PORT_1.21.9_FORGE_EVENT_BUS_FIX_V23.md</summary>

# PF-EMI 1.21.9 Forge Event Bus Fix v23

Forge 59 changed `RegisterClientReloadListenersEvent` so it is no longer an `IModBusEvent`.

The old `@SubscribeEvent` method caused Forge's automatic mod-bus subscriber to attempt to create the event on PF-EMI's mod bus, producing:

`BusGroup "modBusForemi" requires all events on it to inherit from IModBusEvent`

## Fix

- Keep `FMLClientSetupEvent` on PF-EMI's MOD event bus.
- Remove `@SubscribeEvent` from `registerResourceReloaders(...)`.
- Register that handler directly with `RegisterClientReloadListenersEvent.BUS.addListener(...)` during client setup.

No gameplay or EMI recipe/index behavior was changed.
Quilt remains removed; maintained targets are Fabric, Forge, and NeoForge.

</details>

<details>
<summary>PORT_1.21.9_FORGE_JAVAFML_FIX_V22.md</summary>

# PF-EMI 1.21.9 Forge javafml metadata fix v22

Forge 59.0.5 reports javafml 59.0.5. The previous metadata declared `loaderVersion = "[58,59)"`, which excludes every 59.x language-provider version.

Changed:

- `forge/src/main/resources/META-INF/mods.toml`
- `loaderVersion = "[58,59)"` -> `loaderVersion = "[59,60)"`

The existing Forge dependency remains `versionRange = "[59.0.5,60)"`, so the language-provider and Forge dependency ranges now agree on the 59.x line used by Minecraft 1.21.9.

After extracting this patch over the current PF-EMI 1.21.9 source tree, rebuild with `build.bat` and replace the previously built Forge JAR.

</details>

<details>
<summary>PORT_1.21.9_NEOFORGE_CHAR_INPUT_FIX_V21.md</summary>

# PF-EMI 1.21.9 - NeoForge Character Input Fix v21

## Fixed

- Updated the PF-EMI search widget's raw compatibility adapter from `charTyped(char, int)` to `charTyped(int, int)`.
- Minecraft 1.21.9 `CharInput` stores the Unicode code point as an `int`, and NeoForge 21.9 `ScreenEvent.CharacterTyped#getCodePoint()` returns an `int`.
- This removes the NeoForge compile error without truncating supplementary Unicode characters.
- Existing Fabric/Forge/xplat callers that pass a `char` remain source-compatible through Java primitive widening.

## Loader targets

- Fabric
- Forge
- NeoForge

Quilt remains removed from the maintained build/codebase.

</details>

<details>
<summary>PORT_1.21.9_FORGE_SPRITE_FIX_V20.md</summary>

# PF-EMI 1.21.9 loader compile fix v20

This patch is intended to be applied over the current v19 1.21.9 source tree.

## Changes

- Forge: migrated fluid sprite lookup from removed `MinecraftClient#getSpriteAtlas(...)` to the 1.21.9 atlas manager API.
- NeoForge: applied the same migration proactively because the identical removed call was present there too.
- Quilt remains absent. Supported loader targets remain Fabric, Forge, and NeoForge only.

The replacement used by both loader implementations is:

`client.getAtlasManager().getAtlasTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).getSprite(texture)`

This exact call was compiled with `javac` against the cached Minecraft 1.21.9 + Forge 59.0.5 merged jar and the cached Minecraft 1.21.9 + NeoForge 21.9.16-beta merged jar from the uploaded project.

The `ModLoadingContext.get()` message in the Forge build output is a deprecation warning, not the build failure, and is intentionally not changed in this patch.

</details>

<details>
<summary>PORT_1.21.9_FABRIC_LOADER_FIX_V19.md</summary>

# PF-EMI 1.21.9 Fabric / Loader Compile Fix v19

## Fixed from the uploaded full 1.21.9 tree

- Corrected Fabric `IdentifiableResourceReloadListener` to the Minecraft 1.21.9 `reload(Store, Executor, Synchronizer, Executor)` signature and delegated the arguments in that same order.
- Removed the obsolete Fabric `ResourceManager` reload bridge import.
- Removed `ServerPlayerEntity#getServer()` from the Fabric packet receiver.
- Migrated Forge and NeoForge recipe-sync server access through `ServerPlayerEntity#getEntityWorld().getServer()`.
- Migrated NeoForge 21.9 development-environment detection from `FMLLoader.isProduction()` to `FMLEnvironment.isProduction()`.
- Updated the port patcher so it recognizes both the old six-argument reload bridge and the intermediate four-argument `Synchronizer, ResourceManager, Executor, Executor` bridge found in the uploaded tree.
- Aligned `build.sh` with `build.bat`: Fabric, Forge, and NeoForge only.
- Removed active Quilt references from shared source documentation/random recipe text and updated the README to list only the three maintained loaders.

## Validation

The uploaded tree was inspected and patched directly. A local Gradle compile could not be executed in the sandbox because the Gradle wrapper cannot resolve `services.gradle.org`; the environment fails before project configuration. The user's Windows build remains the authoritative compile test.

</details>

<details>
<summary>PORT_1.21.8_HOVER_TOOLTIP_FIX_V15.md</summary>

# PF-EMI 1.21.8 — Forge/NeoForge inventory hover tooltip fix (v15)

## Symptom

On Forge and NeoForge, EMI item hover tooltips in ordinary handled/container screens (for example the player inventory) did not appear. The same hover tooltip worked on Fabric and inside EMI's own recipe screen.

## Cause

Minecraft 1.21.8 uses deferred GUI render-state submission with a separate hover/tooltip stratum. The Forge/NeoForge port split EMI's normal handled-screen render path across two loader events:

- `EmiScreenManager.render(...)` during `ContainerScreenEvent.Render.Foreground`
- `EmiScreenManager.drawForeground(...)` during `ScreenEvent.Render.Post`

The second call was too late for 1.21.8's hover stratum. Fabric did not have the bug because its `HandledScreenMixin` submits both calls together inside `HandledScreen.renderMain(...)`.

## Fix

Forge and NeoForge now submit both `render(...)` and `drawForeground(...)` from the container foreground event, while the screen is still in the correct tooltip submission phase. The late `ScreenEvent.Render.Post` listener and method were removed.

No Fabric/Quilt code was changed.

</details>

<details>
<summary>PORT_1.21.7_RECIPE_SYNC_CODEC_FIX_V13.md</summary>

# PF-EMI 1.21.7 recipe sync codec / Forge reload gate fix (v13)

## Symptoms fixed

- Forge singleplayer could remain forever on `EMI waiting for recipe data from server...` even though the PF-EMI recipe packet arrived.
- The v11 defensive encoder skipped roughly 200 valid vanilla recipes on 1.21.7 with `UnsupportedOperationException: Ingredients can't be empty`.

## Root causes

1. Forge integrated-server connections do not reliably provide PF-EMI with the client-side `TagsUpdatedEvent` needed to complete EMI's two-part tags + recipes reload gate.
2. Minecraft 1.21.7 no longer uses full Recipe packet codecs. Those deprecated packet codecs cannot represent blank cells in many shaped recipes, so valid recipes such as boats, signs, stairs, and anvils fail network encoding.

## Changes

- Forge now also marks tags ready from `ClientPlayerNetworkEvent.LoggingIn`, while retaining `TagsUpdatedEvent` for normal server/tag reloads.
- PF-EMI recipe sync now serializes recipe keys with `Recipe.KEY_CODEC` and recipe values with `Recipe.CODEC`, wrapped through `PacketCodecs.registryCodec(...)`.
- Each recipe is still isolated in a temporary buffer so one genuinely invalid/modded recipe cannot corrupt the entire payload.
- The deprecated `RecipeEntry.PACKET_CODEC` is no longer used by PF-EMI recipe sync.

## Expected result

A Forge integrated-server world should progress past the waiting message and EMI should index the full valid recipe set. The previous mass warnings for normal shaped vanilla recipes should disappear.

</details>

<details>
<summary>PORT_1.21.7_FORGE_RECIPE_SYNC_TIMING_FIX_V12.md</summary>

# PF-EMI 1.21.7 Forge Recipe Sync Timing Fix v12

## Problem

Forge 57 fires `OnDatapackSyncEvent` during player login before the vanilla login recipe/tag synchronization has completed. PF-EMI was sending its custom full-recipe snapshot from that early event. The Forge client payload handler also discarded clientbound EMI packets whenever `MinecraftClient.player` was still null.

The result was a successful game join with EMI permanently displaying:

`EMI waiting for recipe data from server...`

## Fix

- Initial PF-EMI recipe sync now occurs from `PlayerEvent.PlayerLoggedInEvent`, after the Forge login path has completed.
- `OnDatapackSyncEvent` is retained for global datapack reloads (`getPlayer() == null`) so connected clients still receive refreshed recipe data after `/reload`.
- Forge clientbound EMI packets are no longer silently discarded solely because `MinecraftClient.player` is temporarily null. Current PF-EMI S2C packets do not dereference the player argument.

This is Forge-specific. Fabric/Quilt and NeoForge behavior is unchanged.

</details>

<details>
<summary>PORT_1.21.7_RECIPE_SYNC_RUNTIME_FIX_V11.md</summary>

# PF-EMI 1.21.7 Recipe Sync Runtime Fix v11

## Problem

Forge 57.0.3 could launch PF-EMI and connect to normal multiplayer servers, but entering an integrated-server world disconnected with:

`EncoderException: Failed to encode packet 'clientbound/minecraft:custom_payload'`

The nested cause was:

`UnsupportedOperationException: Ingredients can't be empty`

from Minecraft 1.21.7's deprecated full-recipe packet codec while PF-EMI was serializing `RecipeSyncS2CPacket`.

## Fix

`RecipeSyncS2CPacket.write` now pre-encodes each `RecipeEntry` into an isolated temporary `RegistryByteBuf`.

- Recipes that vanilla 1.21.7 can serialize are included unchanged.
- Recipes that vanilla refuses to serialize are skipped instead of corrupting the entire custom payload.
- Up to the first 16 skipped recipe IDs and causes are logged, followed by a total skipped count.
- The temporary buffer prevents a failed partial encode from contaminating the real outgoing packet.

This preserves PF-EMI's full-recipe synchronization for every network-representable recipe while preventing one invalid/empty-tag recipe from disconnecting the player.

</details>

<details>
<summary>PORT_1.21.7_INPUT_RUNTIME_FIX_V10.md</summary>

# PF-EMI 1.21.7 input runtime fix v10

Fixes Forge/NeoForge startup failures caused by the shared MouseMixin/KeyboardMixin targeting vanilla Screen calls that Forge-family loaders replace with their own input hooks.

Changes:
- Forge and NeoForge now load loader-specific copies of the common mixin config with MouseMixin and KeyboardMixin excluded.
- Fabric/Quilt continue using the shared input mixins and the Fabric 1.21.7 refmap.
- Forge routes EMI mouse press/release/drag/scroll and key/character input through ScreenEvent.
- NeoForge routes the same input through NeoForge ScreenEvent and cancels Pre events when EMI consumes them.
- HandledScreenMixin and the rest of the common mixins remain enabled.

</details>

<details>
<summary>PORT_1.21.7_HANDLED_SCREEN_RUNTIME_FIX_V9.md</summary>

# PF-EMI 1.21.7 HandledScreen runtime fix v9

Minecraft 1.21.6/1.21.7 split handled-screen rendering into `render(...)` and `renderMain(...)`.
The vanilla `drawForeground(...)` call now lives in `renderMain(...)`, so the old EMI injector targeting
`render(...)` found zero matching INVOKE instructions and crashed all loaders at startup.

Changes:
- `HandledScreenMixin.renderForeground` now injects into `renderMain(DrawContext,int,int,float)`.
- Fabric/Quilt 1.21.7 refmap maps that selector to intermediary `method_71085`.
- The existing `drawForeground` INVOKE mapping remains `method_2388`.

This patch is intended to be applied after v8.

</details>

<details>
<summary>PORT_1.21.7_FABRIC_MIXIN_RUNTIME_FIX_V8.md</summary>

# PF-EMI 1.21.7 Fabric/Quilt Mixin Runtime Fix v8

This patch fixes the Fabric runtime crash introduced when Loom's legacy Mixin annotation processor was disabled in v5.

## Root cause

The v5 build correctly compiled without the broken legacy annotation processor, but the shared xplat Mixin config no longer had a generated refmap. The Architectury xplat -> Fabric transform remaps Mixin class members, but injector selector strings in the shared Mixin config still require a named -> intermediary refmap in production.

Fabric Loader therefore reached `HandledScreenMixin` with named selectors and reported `No refMap loaded`, causing the first critical injection failure.

## Fix

- Fabric/Quilt now load `emi-fabric-common.mixins.json` instead of the shared `emi.mixins.json`.
- The Fabric-only common config points to `pf-emi-fabric-1.21.7-refmap.json`.
- Forge and NeoForge continue to load the original shared config, so intermediary-only mappings are not injected into their runtime path.
- The refmap is based on PF-EMI's previously generated xplat refmap and updated against Yarn 1.21.7 mappings.
- Updated mappings include:
  - `MinecraftClient.disconnect(Screen, boolean)` -> `method_18096`
  - `StatusEffectsDisplay.drawStatusEffects(DrawContext, int, int)` -> `method_2477`
  - `GameRenderer.guiRenderer` -> `field_59965`
  - `GameRenderer.guiState` -> `field_59966`
  - `GameRenderer.fogRenderer` -> `field_60793`
  - `MinecraftClient.gameRenderer` -> `field_1773`

`useLegacyMixinAp = false` remains enabled; this patch does not restore the compile-time mapping failure fixed by v5.

The Quilt artifact remains the Fabric-compatible artifact and therefore still requires Fabric API 0.129.0+1.21.7 to be installed in the Quilt instance.

</details>

<details>
<summary>PORT_1.21.7_NEOFORGE_PACKET_FIX_V7.md</summary>

# PF-EMI 1.21.7 NeoForge client packet fix v7

Fixes the NeoForge 1.21.7 client-to-server packet dispatch compile failure.

NeoForge moved client-to-server payload sending out of `PacketDistributor` and into the client-only `ClientPacketDistributor` API.

Changed `EmiClientNeoForge` from:

```java
PacketDistributor.sendToServer(EmiPacketHandler.wrap(packet));
```

to:

```java
ClientPacketDistributor.sendToServer(EmiPacketHandler.wrap(packet));
```

and updated the import to `net.neoforged.neoforge.client.network.ClientPacketDistributor`.

No gameplay/network payload format changes were made.

</details>

<details>
<summary>PORT_1.21.7_FORGE_COMPILE_FIX_V6.md</summary>

# PF-EMI 1.21.7 Forge compile fix v6

Fixes the three Forge 57.0.3 Java compilation errors reached after the v5 Mixin mapping fix.

- `IForgeItemStack#getBurnTime` now uses the Forge signature `getBurnTime(RecipeType<?>)`.
- Forge enchantment compatibility now uses `canApplyAtEnchantingTable(Holder/RegistryEntry)` instead of NeoForge-only `supportsEnchantment`.
- Forge payload registration now uses the explicit bidirectional flow builder rather than ambiguous `flow(null)`.

This patch is incremental and is intended to be extracted over the current v4 + v5 1.21.7 project.

</details>

<details>
<summary>PORT_1.21.10_NOTES.md</summary>

# PF-EMI Minecraft 1.21.10 Port

This port advances the current PF-EMI 1.21.9 tree to Minecraft 1.21.10 while preserving the existing Project Flint integration and the completed Fabric/Forge/NeoForge fixes.

## Versions

- Minecraft: `1.21.10`
- Yarn: `1.21.10+build.3`
- Fabric Loader: `0.19.5`
- Fabric API: `0.138.4+1.21.10`
- Forge: `60.1.9`
- NeoForge: `21.10.64`
- Java: `21`
- Architectury Loom: `1.17.493`
- Gradle: `9.5.0`

## Source/API review

The 1.21.9 -> 1.21.10 vanilla migration is small. The relevant vanilla signature change documented for the transition is `BlockBehaviour#entityInside`; PF-EMI does not use that API, so no shared gameplay/recipe source rewrite was required for it.

The existing 1.21.9 migrations for input records, widget clicks, world access, resource reloads, sprite atlas access, Unicode character input, and Forge event-bus registration are retained.

The build toolchain is moved to Architectury Loom 1.17.493 + Gradle 9.5.0 because Forge 1.21.10 has a known SRG remapping conflict on older Architectury Loom builds. Loom 1.17 includes the Forge 1.21.x support fixes.

Forge 60 config screen registration is updated to `MinecraftForge.registerConfigScreen(...)`.

## Packaging

The build targets only Fabric, Forge, and NeoForge. `APPLY_PORT.bat` removes superseded 1.21.9 migration helpers/refmaps from an older source tree before building.

The release output names are:

- `PF-EMI-1.21.10.jar`
- `PF-EMI-1.21.10-FORGE.jar`
- `PF-EMI-1.21.10-NEOFORGE.jar`

## Validation note

The source tree and metadata have been statically validated against the 1.21.10 dependency/API changes. A complete Gradle build could not be executed in the artifact environment because its Gradle dependency download endpoint is unavailable; run `build.bat` on the normal PF-EMI Windows build machine for the authoritative three-loader compile.

## v25 Forge baseline correction

The initial 1.21.10 port declared Forge `60.1.15` as the minimum runtime and compile version. The tested Modrinth instance is running Forge `60.1.9`, and the loader rejected PF-EMI before mod construction solely because of that metadata floor.

The Forge compile baseline is now `1.21.10-60.1.9`, and the runtime dependency range is `[60.1.9,61)`. Building against the minimum claimed Forge version prevents PF-EMI from accidentally compiling against APIs introduced only after 60.1.9.

</details>

<details>
<summary>PORT_1.21.11_NOTES.md</summary>

# PF-EMI Minecraft 1.21.11 Port

Targets: Fabric, Forge, NeoForge. Quilt is retired and is not part of the build.

## Versions

- Minecraft: 1.21.11
- Java: 21
- Yarn: 1.21.11+build.6
- Fabric Loader compile target: 0.19.5 (runtime metadata allows 0.18.1+)
- Fabric API: 0.141.6+1.21.11
- Mod Menu compile-only: 17.0.0
- Forge: 1.21.11-61.2.0
- NeoForge: 21.11.45
- Architectury Loom: 1.17.493
- Gradle wrapper: 9.5.0

The Architectury Forge/NeoForge mapping patch coordinates intentionally remain on the newest published bridge artifacts used by the project (`forge_yarn_patch=1.21.9+build.6`, `neoforge_yarn_patch=1.21+build.6`). They are mapping bridges, not the target Minecraft version.

## 1.21.11 source/resource changes included

- Updated `DrawContext` construction for the 1.21.11 GUI render-state constructor.
- Advanced Fabric runtime refmap naming to 1.21.11 while preserving the verified intermediary targets carried from the 1.21.10 port.
- Resource pack metadata advanced from format 69 to format 75.
- Loader metadata and build scripts target 1.21.11 only.
- Existing Forge global reload-event bus registration and Forge/NeoForge 1.21.9/1.21.10 compatibility fixes are retained.

Run `APPLY_PORT.bat`, then `build.bat`. Final jars are written to `release/`.

</details>

<details>
<summary>PORT_1.21.3_NOTES.md</summary>

# PF-EMI 1.21.3 - Initial Forward Port

Initial forward-port baseline from the working 1.21.2 source.

Changes in this pass:

- Minecraft target: `1.21.2` -> `1.21.3`
- Yarn mappings: `1.21.2+build.1` -> `1.21.3+build.2`
- Fabric API: `0.106.1+1.21.2` -> `0.106.1+1.21.3`
- NeoForge: `21.2.0-beta` -> `21.3.0-beta`
- Fabric metadata now requires Minecraft 1.21.3.
- NeoForge metadata now accepts the 21.3.x loader line.
- Resource pack format remains 42 for 1.21.3.
- Clean builder filenames automatically become `PF-EMI-1.21.3.jar` and `PF-EMI-1.21.3-NEOFORGE.jar`.
- The complete 1.21.2 compatibility code is retained as the baseline because 1.21.3 is a bug-fix follow-up to the same rendering/recipe generation.

Next step: run `build.bat` and use the first compiler output as the authoritative API delta for the 1.21.3 pass.

</details>

<details>
<summary>PORT_1.21.4_NOTES.md</summary>

# PF-EMI 1.21.4 - Forward Port Pass 1

Base: working PF-EMI 1.21.3 source.

## Target updates

- Minecraft: `1.21.3` -> `1.21.4`
- Yarn: `1.21.4+build.8`
- Fabric API: `0.114.1+1.21.4`
- NeoForge: `21.4.140`
- NeoForge resource pack format: `46`
- Data pack format for Minecraft 1.21.4: `61`

## 1.21.4 rendering compatibility

Minecraft 1.21.4 replaces the older item-model rendering pipeline with `ItemModelManager` / `ItemRenderState`. PF-EMI's legacy stack batcher was already disabled on the 1.21.2 port because it caused visual corruption, so this port removes the now-obsolete BakedModel/ItemRenderer batching internals entirely and keeps vanilla `DrawContext` item rendering as the only active path.

Custom EMI tag-model registration is also disabled for this target. Tag ingredients continue to use their representative item icon, matching the safe fallback used by the 1.21.2/1.21.3 port.

## Build outputs

`build.bat` automatically emits only:

- `release/PF-EMI-1.21.4.jar`
- `release/PF-EMI-1.21.4-NEOFORGE.jar`

Run `build.bat`; any remaining compiler output is the next authoritative 1.21.4 API delta.

</details>

<details>
<summary>PORT_1.21.4_PASS2_NOTES.md</summary>

# PF-EMI 1.21.4 - Port Pass 2

Fixes the Minecraft 1.21.4 `Ingredient#getMatchingItems()` API change.

In 1.21.4, `getMatchingItems()` returns a `Stream<RegistryEntry<Item>>`, so PF-EMI must consume the stream directly instead of calling collection methods on it.

Modified:
- `xplat/src/main/java/dev/emi/emi/api/stack/EmiIngredient.java`

The stream is converted once into `List<EmiStack>`, checked for emptiness, and passed to EMI tag handling.

</details>

<details>
<summary>PORT_1.21.4_PASS3_NOTES.md</summary>

# PF-EMI 1.21.4 - Pass 3

Build launcher fix for machines where Java 25 appears before Java 21 in PATH.

## Changes

- `build.bat` no longer assumes the first `java.exe` in PATH is suitable.
- The launcher actively locates and verifies a Java 21 JDK.
- Search order includes `PFEMI_JAVA21_HOME`, `JAVA_HOME`, PATH Java installations, and common Windows JDK install directories.
- Gradle is forced to the verified JDK through `JAVA_HOME`, `JDK_HOME`, `GRADLE_JAVA_HOME`, PATH, and `-Dorg.gradle.java.home`.
- If Java 21 is not installed, the launcher stops before Gradle and prints a clear installation/override message.
- Release naming remains `PF-EMI-<mc>.jar` and `PF-EMI-<mc>-NEOFORGE.jar`.

The source port changes from pass 2 are otherwise unchanged.

</details>

<details>
<summary>PORT_1.21.4_PASS4_NOTES.md</summary>

# PF-EMI 1.21.4 Port - Pass 4

This pass fixes the NeoForge-only compile failures exposed after xplat compiled successfully.

## Changes

- `RegisterClientReloadListenersEvent` -> `AddClientReloadListenersEvent`.
- Reload listeners now use NeoForge's sorted reload listener API:
  - `event.addListener(reloader.getEmiId(), reloader)`
  - PF-EMI's `EmiResourceReloadListener#getEmiId()` provides the required stable listener ID.
- Block atlas lookup moved from removed `PlayerScreenHandler.BLOCK_ATLAS_TEXTURE` to `SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE`.

The `EventBusSubscriber.Bus.MOD` messages are deprecation warnings and are not build failures.

</details>

<details>
<summary>PORT_1.21.5_NOTES.md</summary>

# PF-EMI 1.21.5 Port — Pass 1

Starting point: the working Minecraft 1.21.4 pass-4 tree.

Initial retarget:
- Minecraft 1.21.5
- Yarn 1.21.5+build.1
- Fabric API 0.119.5+1.21.5
- Mod Menu 14.0.2
- NeoForge 21.5.98
- NeoForge dependency range 21.5.x
- Resource-pack format 55
- Java 21 remains mandatory and build.bat still actively locates/forces a Java 21 JDK.

This is the first compile-discovery pass. 1.21.5 is a larger technical update than 1.21.4, so compiler/runtime feedback is expected to identify additional source migrations.

</details>

<details>
<summary>PORT_1.21.5_PASS1_NOTES.md</summary>

# PF-EMI 1.21.5 — Port Pass 1

Forward-port baseline: the working Minecraft 1.21.4 pass-4 source.

Retargeted build/runtime metadata:
- Minecraft 1.21.5
- Yarn 1.21.5+build.1
- Fabric API 0.119.5+1.21.5
- Fabric Loader 0.17.3
- Mod Menu 14.0.2 (compile-only)
- NeoForge 21.5.98
- NeoForge dependency range [21.5.0-beta,21.6)
- Resource-pack format 55

Preserved the Java 21-enforcing build.bat and the clean release naming:
- PF-EMI-1.21.5.jar
- PF-EMI-1.21.5-NEOFORGE.jar

The 1.21.4 compatibility work is retained as the code baseline. This pass intentionally starts the 1.21.5 compiler-discovery loop; additional source migrations will be applied from the first local compiler log.

</details>

<details>
<summary>PORT_1.21.5_PASS2_NOTES.md</summary>

# PF-EMI 1.21.5 — Port Pass 2

Compiler batch addressed:
- `ShaderProgramKeys` no longer exists in the old 1.21.4 package/API.
- `BufferRenderer` was removed from the old GUI rendering path.
- `VertexFormat` moved to `com.mojang.blaze3d.vertex`.
- `GlStateManager` is no longer needed by EMI's config-list rendering.

Changes:
- Converted the old shader-selection helpers in `EmiPort` to compatibility no-ops. 1.21.5 GUI rendering now selects shaders through `RenderLayer` / `RenderPipeline`.
- Updated tinted sprite rendering to draw its built buffer through `RenderLayer.getGuiTextured(...).draw(...)`.
- Updated `VertexFormat.DrawMode` to the new `com.mojang.blaze3d.vertex.VertexFormat.DrawMode` package.
- Replaced the custom low-level config list scrollbar/selection quads with `DrawContext.fill(...)` so they use the 1.21.5 GUI pipeline.
- Removed the stale direct `RenderSystem.setShaderTexture(... Identifier ...)` call; `DrawContext` / `RenderLayer` owns texture binding in 1.21.5.
- Preserved the Java 21-enforcing build script and clean release names.

Local Gradle validation is still required because this environment cannot resolve `services.gradle.org`.

</details>

<details>
<summary>PORT_1.21.5_PASS3_NOTES.md</summary>

# PF-EMI 1.21.5 Port Pass 3

Compile fixes for the second 1.21.5 xplat error batch.

- PlayerInventory main storage now uses getMainStacks().
- StringNbtReader migration uses readCompound(); encoded NBT uses toString().
- RenderTickCounter getTickDelta(false) migrated to getTickProgress(false).
- Removed obsolete global RenderSystem depth/blend/color-mask mutation paths.
- Recipe screenshot capture migrated to the 1.21.5 GPU framebuffer model:
  - named SimpleFramebuffer constructor
  - framebuffer redirect through a mutable MinecraftClient accessor
  - CommandEncoder clearColorAndDepthTextures
  - vanilla ScreenshotRecorder.takeScreenshot asynchronous readback
  - ClickEvent.OpenFile
- Smithing recipe base ingredient is now direct rather than Optional.
- DyedColorComponent now takes only RGB.

Local Gradle validation remains authoritative because the assistant environment cannot reach the Gradle distribution host.

</details>

<details>
<summary>PORT_1.21.5_PASS4_NOTES.md</summary>

# PF-EMI 1.21.5 Port - Pass 4

NeoForge compile follow-up after pass 3.

## Fixes

- Updated NeoForge GUI render callbacks from `RenderTickCounter#getTickDelta(false)` to the 1.21.5 `getTickProgress(false)` API.
- Replaced the removed NeoForge `ItemStack#isBookEnchantable(ItemStack)` hook with `ItemStack#supportsEnchantment(RegistryEntry<Enchantment>)`. This is NeoForge's stack-aware applicability hook for anvil/book-style enchantment application.
- Removed the now-unused `Items.ENCHANTED_BOOK` compatibility construction.

The `EventBusSubscriber.Bus.MOD` messages are deprecation warnings only and are intentionally left alone in this compile pass.

</details>

<details>
<summary>PORT_1.21.6_NOTES.md</summary>

# PF-EMI 1.21.6 — Port Pass 1

This pass carries the working Minecraft 1.21.5 pass-4 tree forward to Minecraft 1.21.6 and performs the first source migration for the major 1.21.6 GUI renderer rewrite.

## Target

- Minecraft 1.21.6
- Yarn 1.21.6+build.1
- Fabric Loader 0.17.3
- Fabric API 0.128.2+1.21.6
- Mod Menu 15.0.0
- NeoForge 21.6.20-beta
- Resource-pack format 63
- Java 21

## Ported in this pass

- Retargeted Fabric and NeoForge metadata to 1.21.6.
- Preserved the Java-21-enforcing Windows builder and automatic release naming.
- Migrated EMI's GUI transform compatibility layer from the old 4x4 `MatrixStack` model to the 1.21.6 `Matrix3x2fStack` used by `DrawContext`.
- Mapped old positive GUI Z translations to new root GUI layers with `DrawContext#createNewRootLayer()`.
- Moved recipe-tree, search-widget, and hovered-craftable transforms onto the `DrawContext` matrix stack so deferred 1.21.6 GUI rendering receives the correct transforms.
- Migrated GUI texture submission from `RenderLayer` factories to `RenderPipelines.GUI_TEXTURED`.
- Migrated tinted fluid/sprite drawing to the deferred `DrawContext` sprite pipeline.
- Migrated custom tooltip text submission to `TooltipComponent#drawText(DrawContext, TextRenderer, int, int)`.
- Updated the private tooltip invoker to the new focused-flag signature.
- Ensured EMI text helpers add an opaque alpha channel when callers provide legacy RGB-only colors; 1.21.6 treats zero alpha as transparent.
- Kept old global shader/depth/blend helpers as compatibility no-ops because GUI state is now owned by render pipelines/render state.

## Expected next step

Minecraft 1.21.6 substantially rewrote GUI preparation/rendering and GPU projection handling. This is the first compile-discovery pass; run `build.bat` locally and use the first compiler batch to finish any remaining framebuffer, input, mixin, or loader-specific API changes.

</details>

<details>
<summary>PORT_1.21.7_MAPPING_FIX_NOTES.md</summary>

# PF-EMI 1.21.7 mapping/AP fix (v5)

This patch fixes the `:xplat:compileJava` failure that appeared after Forge was enabled.

## Root cause

The shared xplat module was still using Loom 1.11's legacy Mixin annotation processor. With Fabric/Yarn intermediary mappings and Forge present in the same Architectury build, the AP failed to load its `named:intermediary` mapping input. That caused every `@Inject`, `@Shadow`, `@Accessor`, and `@Invoker` in xplat to report missing obfuscation mappings even though those mixins were not individually broken.

## Fix

`loom.mixin.useLegacyMixinAp = false` is now set in:

- `xplat/build.gradle`
- `fabric/build.gradle`

Mixin targets are therefore remapped by Loom/TinyRemapper at jar-remap time instead of the failing legacy annotation-processor pass.

No gameplay code or mixin target descriptors were changed by this patch.

</details>

<details>
<summary>PORT_1.21.7_NOTES.md</summary>

# PF-EMI 1.21.7 Port Notes

PF-EMI is targeted at **Minecraft 1.21.7** with Fabric, Quilt-compatible, Forge, and NeoForge release artifacts.

## Dependency targets

- Minecraft 1.21.7
- Yarn 1.21.7+build.7
- Fabric Loader 0.17.3
- Fabric API 0.129.0+1.21.7
- Forge 1.21.7-57.0.3
- NeoForge 21.7.25-beta
- Architectury Loom 1.11.458
- Architect Plugin 3.4.162
- Java 21

## Loader work

- Fabric remains a native build target.
- Quilt uses the Fabric build unchanged because modern Quilt Loader supports Fabric mods; a separately named Quilt artifact is emitted for release organization.
- Re-enabled the dormant Forge module and ported it to the current PF-EMI shared API.
- Replaced Forge's stale pre-1.21.2 packet registration with Forge custom-payload channels and registered PF-EMI recipe sync.
- Ported Forge brewing enumeration to the current world brewing registry, including Forge-added `IBrewingRecipe` entries.
- Ported Forge screen rendering to the 1.21.7 render tick API.
- Added Forge datapack recipe synchronization to match Fabric/NeoForge PF-EMI behavior.
- Updated Forge fluid rendering/name bridging for Forge 57's loader-specific fluid stack API.
- Retained NeoForge 21.7 and the previously completed 1.21.6 GUI/screenshot and event-bus compatibility fixes.
- Updated resource pack metadata to Minecraft 1.21.7 resource pack format 64.

## Build output

`build.bat` builds all native modules and emits:

- `release/PF-EMI-1.21.7.jar`
- `release/PF-EMI-1.21.7-QUILT.jar`
- `release/PF-EMI-1.21.7-FORGE.jar`
- `release/PF-EMI-1.21.7-NEOFORGE.jar`

The Quilt artifact is copied from the final Fabric jar by design.

## Forge compatibility note

Forge 57 still exposes Forge `FluidStack` metadata separately from PF-EMI's shared vanilla-component stack representation. The Forge bridge preserves the fluid and amount, while Forge-only fluid NBT is not represented in the shared PF-EMI `ComponentChanges` payload. Standard fluids render and index normally; a mod relying on distinct Forge-fluid NBT variants may need a loader-specific extension later.

## Validation

The Forge source was audited against Forge 57/EventBus 7 APIs and scanned for the obsolete APIs from the dormant module. The build environment used for this patch cannot resolve `services.gradle.org`, so a complete Gradle compile was not possible here. `build.bat` is configured to compile Fabric, Forge, and NeoForge and then emit the Quilt-compatible Fabric copy.

</details>

<details>
<summary>PORT_1.21.8_NOTES.md</summary>

# PF-EMI Minecraft 1.21.8 Port Notes

This port starts from the cumulative Minecraft 1.21.7 v13 state and preserves all runtime/loader fixes made during that port.

## Target versions

- Minecraft: 1.21.8
- Yarn: 1.21.8+build.1
- Fabric Loader: 0.17.3 minimum/build baseline
- Fabric API: 0.136.1+1.21.8
- Forge build target: 1.21.8-58.1.22
- Forge metadata compatibility: 58.1.x through the 58.x line
- NeoForge build target: 21.8.54
- NeoForge metadata compatibility: 21.8.9 through the 21.8.x line
- Java: 21

## Carried forward from 1.21.7

- 1.21.6+ deferred GUI screenshot renderer port using `GuiRenderState`/`GuiRenderer`.
- Correct `MinecraftClient.disconnect(Screen, boolean)` and status-effects display signatures.
- Fabric/Quilt non-legacy Mixin AP setup plus handwritten production refmap.
- `HandledScreenMixin` foreground injection through `renderMain`.
- Forge/NeoForge loader-native mouse and keyboard input handling instead of vanilla-dispatch Mixins.
- NeoForge client-to-server packets through `ClientPacketDistributor`.
- Forge EventBus 7 and custom-payload networking port.
- PF-EMI full recipe synchronization using registry-aware recipe data codecs instead of Minecraft's obsolete full-recipe packet codecs.
- Per-recipe sync isolation so a genuinely unserializable modded recipe cannot disconnect the client.
- Forge login/reload recipe-sync timing and client reload-gate fixes.

## 1.21.8-specific work

Minecraft 1.21.8 is a hotfix over 1.21.7. The PF-EMI Mixin/accessor targets used by the Fabric/Quilt handwritten refmap were checked against Yarn 1.21.8 and retain their intermediary IDs, so no Java Mixin selector rewrite was required for this pass.

The Fabric refmap resource has been renamed to `pf-emi-fabric-1.21.8-refmap.json` to keep the production jar self-describing.

Forge and NeoForge loader APIs used by the current PF-EMI port were audited against their 1.21.8 branches; the current networking, screen-input, reload, and login hooks remain available.

## Quilt

The Quilt release remains the Fabric artifact copied under the `-QUILT.jar` name. Fabric API 0.136.1+1.21.8 must be installed in the Quilt instance because PF-EMI's Fabric implementation directly uses Fabric API.

## Validation

Run `build.bat` and test, at minimum:

1. Fabric startup and inventory/recipe UI.
2. Quilt startup with Fabric API installed.
3. Forge singleplayer recipe synchronization and EMI reload completion.
4. Forge mouse/keyboard interaction inside handled screens.
5. NeoForge startup, handled-screen input, and client/server recipe synchronization.
6. A ProjectFlint instance to confirm PF-EMI-specific recipe/index/multiblock integration still loads normally.

</details>

<details>
<summary>PORT_1.21.9_FABRIC_NOTES.md</summary>

# PF-EMI 1.21.9 Fabric Port

## Scope

This pass ports the current PF-EMI 1.21.8 v15 state to Minecraft 1.21.9 for Fabric only.
Forge, NeoForge, and the Quilt-labelled artifact are intentionally not built by this pass. They will be ported separately from this shared 1.21.9 baseline.

## Target versions

- Minecraft: 1.21.9
- Yarn: 1.21.9+build.1
- Fabric Loader: 0.17.3
- Fabric API: 0.134.1+1.21.9
- Mod Menu compile-only integration: 16.0.0
- Java: 21

## Major 1.21.9 source change

Minecraft 1.21.9 replaced several raw GUI input argument lists with input records.
PF-EMI was updated for the new API:

- keyboard input now uses `KeyInput`
- typed character input now uses `CharInput`
- low-level mouse button input now uses `MouseInput`
- screen mouse click/release/drag callbacks now use `Click`
- EMI's internal integer-based input API remains intact through small adapter overloads where useful

The Fabric production refmap was regenerated/updated for the 1.21.9 method descriptors while retaining the verified intermediary names that remained stable from 1.21.8.

## Build isolation

`enabled_platforms=fabric` is used for this pass and `settings.gradle` now includes loader projects conditionally. This prevents the still-1.21.8 Forge and NeoForge modules from being configured while compiling the Fabric 1.21.9 port.

`build.bat` and `build.sh` intentionally build only:

`release/PF-EMI-1.21.9.jar`

## Carried-forward fixes

This port keeps the current 1.21.8 v15 behavior, including:

- deferred GUI screenshot rendering changes
- Fabric production mixin/refmap fix
- `HandledScreen.renderMain` integration
- Project Flint full recipe synchronization using registry-backed recipe codecs
- per-recipe sync isolation
- all prior Project Flint EMI integration behavior

The Forge/NeoForge native input and tooltip fixes remain in their source trees but those modules are not part of this Fabric-only build pass.

</details>

<details>
<summary>PORT_1.21.9_FORGE_MAPPING_FIX.md</summary>

# PF-EMI 1.21.9 Forge Mapping Fix

This hotfix addresses the Forge configuration failure that occurs before Java compilation:

- `Failed to setup Minecraft`
- `Failed to remap minecraft`
- `Mapping target name conflicts detected`
- `METHODs ... [getFluid, a]()... -> getFluid`
- `There were unfixable conflicts.`

## Cause

The Forge module was still consuming raw Yarn mappings directly. Forge 1.21.9 / 59.0.5 needs Architectury's Forge-specific Yarn mapping bridge layered after Yarn when building this Architectury Loom-based PF-EMI tree.

## Applied change

`gradle.properties` gains:

```properties
forge_yarn_patch=1.21.9+build.6
```

and `forge/build.gradle` changes from the raw Yarn mapping declaration to:

```groovy
mappings loom.layered {
	it.mappings("net.fabricmc:yarn:${rootProject.yarn_mappings}:v2")
	it.mappings("dev.architectury:yarn-mappings-patch-forge:${rootProject.forge_yarn_patch}")
}
```

NeoForge is intentionally unchanged and remains on its existing Architectury NeoForge patch layer.

## Apply

Extract these files into the PF-EMI 1.21.9 project root, run `APPLY_FORGE_MAPPING_FIX.bat`, then run the existing `build.bat` again.

</details>

<details>
<summary>PORT_1.21.9_LOADER_FIX.md</summary>

# PF-EMI 1.21.9 Loader Project Fix — v17

This patch fixes the Gradle project-graph failure where `build.bat` requested `:forge:*` and `:neoforge:*` tasks but `settings.gradle` did not register those projects.

## Behavior

- `build.bat` automatically runs the loader repair before Gradle.
- Ensures `settings.gradle` contains Fabric, Forge, and NeoForge projects.
- Removes the Quilt project and Quilt registration.
- Retargets the loader coordinates/metadata to Minecraft 1.21.9.
- If the current 1.21.9 tree is missing the Forge or NeoForge module folder entirely, the repair script attempts to restore that module from a sibling PF-EMI 1.21.8 project folder before retargeting it.
- Validates the project graph before starting the actual build.

The expected final release names remain:

- `PF-EMI-1.21.9.jar`
- `PF-EMI-1.21.9-FORGE.jar`
- `PF-EMI-1.21.9-NEOFORGE.jar`

</details>

<details>
<summary>PORT_1.21.9_NOTES.md</summary>

# PF-EMI 1.21.9 Port — v18

This overlay advances the Minecraft **1.21.9** port after the first Fabric + Forge + NeoForge build reached the shared `:xplat:compileJava` stage.

## Active targets

- Fabric
- Forge
- NeoForge

Quilt is removed rather than disabled. The patch deletes a remaining `quilt/` module, removes it from `settings.gradle`, rejects retired-loader references in maintained build/code inputs, and never emits a Quilt jar.

## Version coordinates

- Minecraft: `1.21.9`
- Yarn: `1.21.9+build.1`
- Fabric API: `0.134.1+1.21.9`
- Forge: `1.21.9-59.0.5`
- NeoForge: `21.9.16-beta`
- Java: 21

## Shared-source fixes added in v18

The compiler log showed 18 errors caused by 1.21.9 API changes. v18 migrates those exact call sites:

- key polling: raw window handle -> `Window`;
- Ctrl/Command detection: removed `Screen.hasControlDown()` -> explicit 1.21.9 key polling;
- macOS detection: removed `MinecraftClient.IS_SYSTEM_MAC` -> `Util.getOperatingSystem()`;
- player world access: `getWorld()` -> `getEntityWorld()`;
- world side test: direct `world.isClient` field -> `world.isClient()`;
- deferred tooltips: `renderTooltip()` -> `drawDeferredElements()`;
- vanilla widget clicks: `(mouseX, mouseY, button)` -> `Click` / `MouseInput`;
- search formatting: `setRenderTextProvider` -> `addFormatter`;
- sidebar button callback: `onPress()` -> `onPress(AbstractInput)`.

These are compatibility changes only. PF-EMI's recipe synchronization, ProjectFlint integration behavior, sidebar/index behavior, and fork identity are not replaced with upstream implementations.

## Release outputs

- `release/PF-EMI-1.21.9.jar`
- `release/PF-EMI-1.21.9-FORGE.jar`
- `release/PF-EMI-1.21.9-NEOFORGE.jar`

## Application

Extract this ZIP directly into the existing PF-EMI 1.21.9 source root, with no extra wrapper directory. Run `APPLY_PORT.bat`, then run `build.bat`.

## v18 Fabric compiler pass

- Updated Fabric's resource reload adapter to Minecraft 1.21.9's four-argument `ResourceReloader.reload(Store, Executor, Synchronizer, Executor)` signature.
- Removed obsolete `ResourceManager` / `Profiler` reload bridge imports.
- Removed `ServerPlayerEntity#getServer().execute(...)` from the Fabric payload receiver; Fabric invokes this receiver on the server thread, so the packet is applied directly.
- Quilt remains completely removed. Maintained targets are Fabric, Forge, and NeoForge only.

</details>

<details>
<summary>PORT_NOTES_1.21.6.txt</summary>

PF-EMI Minecraft 1.21.6 rendering/API patch

Changed:
- Ported recipe screenshot rendering to Minecraft 1.21.6's deferred GuiRenderState/GuiRenderer pipeline.
- Removed the pre-1.21.6 DrawContext constructor usage.
- Removed the pre-1.21.6 direct Matrix4f projection manipulation from recipe screenshots.
- Added GameRenderer accessors for guiRenderer, guiState, and fogRenderer.
- Updated MinecraftClient.disconnect Mixin target to disconnect(Screen, boolean).
- Updated StatusEffectsDisplay.drawStatusEffects Mixin target to drawStatusEffects(DrawContext, int, int).

Validation:
- API/signature changes were checked against Yarn 1.21.6+build.1 documentation.
- The sandbox cannot download Gradle/dependencies, so a full Gradle compile could not be executed here.

</details>


### Historical passes original notes

<details>
<summary>PASS4_NOTES.md</summary>

# PF-EMI 1.21.2 Compile Pass 4

Fixes the four remaining xplat compile errors in `InventoryRecipeHandler`.

Minecraft 1.21.2 moved crafting-grid dimensions onto `AbstractCraftingScreenHandler`:
- `getWidth()`
- `getHeight()`

`AbstractRecipeScreenHandler` no longer exposes the old `getCraftingWidth()` / `getCraftingHeight()` API.

Changes:
- `AbstractRecipeScreenHandler` type checks -> `AbstractCraftingScreenHandler`
- `getCraftingWidth()` -> `getWidth()`
- `getCraftingHeight()` -> `getHeight()`

</details>

<details>
<summary>PASS6_NOTES.md</summary>

# PF-EMI 1.21.2 compile pass 6

Fixes the Fabric `IdentifiableResourceReloadListener` compile failure introduced by Minecraft 1.21.2's `ResourceReloader` signature change.

- Removed the obsolete `Profiler` parameters from `EmiClientFabric.reload(...)`.
- Removed the obsolete `net.minecraft.util.profiler.Profiler` import.
- Forwards the 1.21.2 four-argument reload call directly to EMI's registered resource reloader.

Apply this archive directly over the existing PF-EMI 1.21.2 project root.

</details>

<details>
<summary>PASS7_NOTES.md</summary>

# PF-EMI 1.21.2 - Compile Pass 7

This pass targets the NeoForge-only compile errors reached after Fabric compiled successfully.

Changes:
- Removed the unavailable NeoForge `RecipesUpdatedEvent` client listener.
- Moved PF-EMI's authoritative recipe-object packet to `OnDatapackSyncEvent`, so recipes are sent on player join and again after server datapack reloads.
- Updated NeoForge `FluidIngredient` conversion from removed `getStacks()` to `fluids()` and converts the accepted fluid holders into display `FluidStack`s.
- Updated NeoForge fuel lookup to pass Minecraft 1.21.2's synchronized `FuelRegistry` into `ItemStack#getBurnTime`.

The complete Gradle compile cannot be executed in the artifact environment because external Gradle distribution resolution is DNS-blocked. Run `build.bat` locally to verify the next layer.

</details>

<details>
<summary>PASS8_NOTES.md</summary>

# PF-EMI 1.21.2 - Runtime Mixin Pass 8

This pass fixes the first runtime-only failures after Fabric and NeoForge both compiled successfully.

## Fixed

- `MouseMixin`
  - Minecraft 1.21.2 removed the `Screen.wrapScreenError(...)` calls from mouse input handling.
  - PF-EMI now injects immediately before the direct 1.21.2 calls to:
    - `Screen.mouseClicked(double, double, int)`
    - `Screen.mouseReleased(double, double, int)`
    - `Screen.mouseDragged(double, double, int, double, double)`
    - `Screen.mouseScrolled(double, double, double, double)`
  - This fixes the NeoForge startup crash reporting `MouseMixin ... failed injection check (0/1)`.

- Fabric `ClientPlayNetworkHandlerMixin`
  - Removed the obsolete injection into vanilla recipe synchronization.
  - PF-EMI 1.21.2 already synchronizes full recipe objects with `RecipeSyncS2CPacket`; the packet itself updates `EmiSyncedRecipes` and calls `EmiReloadManager.reloadRecipes()`.
  - This fixes the Fabric startup crash reporting `onSynchronizeRecipes ... failed injection check (0/1)`.

- `KeyboardMixin`
  - Proactively ported the same 1.21.2 input-path change from `Screen.wrapScreenError(...)` to the direct `Screen.keyPressed(int, int, int)` call.
  - Prevents the equivalent keyboard mixin failure from becoming the next runtime crash.

## Build script

The full source package keeps the latest clean-release `build.bat` naming behavior requested for PF-EMI builds.

</details>

<details>
<summary>PASS9_NOTES.md</summary>

# PF-EMI 1.21.2 - Runtime Pass 9

## Rendering correctness

- Disabled EMI's legacy batched item renderer for the 1.21.2 port. The batcher was reconstructed from pre-render-rewrite code and did not preserve 1.21.2 GUI model/depth/lighting state consistently.
- All item icons now go through Minecraft 1.21.2 `DrawContext` rendering on Fabric and NeoForge.
- Removed the manual `DiffuseLighting.enableGuiDepthLighting()` call before normal item draws so item rendering no longer leaks lighting state into later EMI widgets.
- Tightened the tag fallback renderer so it draws only the representative icon; tag amount/remainder/tag overlays are rendered exactly once by the tag ingredient itself.

This pass targets the visual corruption/missing-model symptoms seen after the port first became runtime-stable.

</details>

<details>
<summary>PATCH_NOTES.md</summary>

# PF-EMI 1.21.2 compile pass 3

This patch contains actual Java source changes for the compiler errors from the 2026-09-05 build log.

Main changes:
- Add EmiDrawContext overloads to the 1.21.2 DrawContext texture compatibility bridge.
- Port TooltipComponent to the 1.21.2 TextRenderer-aware height/drawItems API.
- Port TagKey/Registry access to registryRef(), iterateEntries(), and 1.21.2 tag streams.
- Port shader selection to ShaderProgramKeys.
- Port framebuffer/projection APIs and Toast API.
- Port ItemRenderer model lookup, VertexBuffer GlUsage, stack-overlay rendering, and recipe handler generics.
- Keep custom tag-model rendering on a safe first-stack fallback for 1.21.2.
- Keep EMI profiling hooks as no-ops because MinecraftClient no longer exposes getProfiler() in 1.21.2.

Extract this ZIP over the PF-EMI 1.21.2 project root and replace files.

</details>

<details>
<summary>PF_EMI_NOTICE.md</summary>

# PF-EMI

ProjectFlint-maintained EMI fork. Current port target: Minecraft 1.21.11.

Supported build targets in this source tree are Fabric, Forge, and NeoForge only. Quilt support is retired.

</details>

<details>
<summary>PORT_STATUS.md</summary>

# PF-EMI Port Status

Current target: Minecraft 1.21.11

Maintained loaders:
- Fabric
- Forge
- NeoForge

Quilt is retired and must not be restored to the project graph or release builder.

Expected outputs:
- `PF-EMI-1.21.11.jar`
- `PF-EMI-1.21.11-FORGE.jar`
- `PF-EMI-1.21.11-NEOFORGE.jar`

</details>

