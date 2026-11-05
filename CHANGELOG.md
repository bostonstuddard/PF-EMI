# PF-EMI — Unified Changelog

This is the single changelog for the ProjectFlint compatibility branch of EMI. Minecraft **1.21.11**, Fabric is the supported build target for the packaged `build.bat`. Entries describe **PF-EMI compatibility features**, not changes to ProjectFlint gameplay itself. For release readiness and exact gameplay parity, see [`docs/PROJECTFLINT_EMI_AUDIT_CHECKLIST.md`](docs/PROJECTFLINT_EMI_AUDIT_CHECKLIST.md).

## 1.1.23-pf.18 — ProjectFlint 0.31.57-b and player-facing recipe cleanup (2026-11-05)

- Updated the ProjectFlint compatibility reference to commit `7e7321a06daff837866c6ba95c4436f2e83c8824` (0.31.57-b), including the 0.31.56 physical Basin update.
- Replaced the old wall-of-text Basin pages with compact input/station/output layouts. Each shows the exact 2–3 stomp requirement, one food point per stomp, and per-output odds on hover; mutually exclusive quantities are differentiated from independent bonus rolls.
- Corrected all Basin material rules to 0.31.57-b, including Cobblestone, Clay, Rare Earth Dust, Tin Dust, new bonus drops, and Stone's possible zero-output washes. Wood Dust and Tannin remain in the same category.
- Shortened other world/loot guide screens to a maximum of two visible hint lines. Additional useful notes are accessible by hovering the source item. Multi-row drop pages no longer hide results beyond the sixth slot.
- Added Fireclay Forge processing for MBSSIG Dust + a full Charcoal burn → Pure Mica Insulated Glass. The existing Hardwood Alloy/Charcoal pathways remain.
- Added the first Hardwood Crafting Table as personal 2×2 crafting (2 Hardwood Alloy, Cobblestone, Bound Wood Processor); corrected personal Leather Strap and Repair Kit counts. Dedicated Hardwood Table recipes, including the Borosilicate/insulated-glass dust chain, continue to be discovered from the installed ProjectFlint custom recipe type or bundled JSON fallback.
- Improved reusable-tool recognition for Hardwood durable recipes with both Bone Knife and Flint File, avoiding misleading consumed-tool entries.
- **Validation:** offline source-contract checks and Java syntax parsing; no full Gradle build or game/GUI test was available here. Fabric only.

## 1.1.23-pf.17 — ProjectFlint 0.31.55-b integration (2026-11-04)

- Pinned compatibility reference: `bostonstuddard/ProjectFlint` commit `54184ab574bc1999ad616d91518d4681365fca96` (`0.31.55-b`), incorporating v0.31.53-b and 0.31.54-b changes.
- Added ten Basin material processes with **all individual independent output chances** displayed in hover text, including raw metals, gravel/sand, stone, fertilizer, Slag recycling and zero-drop outcomes. Each operation consumes one food point and requires the player to jump on the Basin; these are not normal crafting recipes.
- Added Brick Furnace **Cobblestone → Stone** (300 base ticks, 200 effective ticks at 1.5× furnace speed); not available in Brick Blast Furnace or primitive kilns.
- Hardwood 3×3 recipes continue to be indexed from the installed ProjectFlint `hardwood_crafting` type / bundled recipe JSON fallback; this automatically includes the **Axle**, alternate Basin, revised Hardwood Sticks tool recipes, corrected slabs, and stone-group stairs/slabs. Retired/renamed recipes are not hardcoded back into EMI.
- Restored actual early 2×2 Hinged Door recipes with three planks and one Wooden Door Hinge for each of nine overworld wood types; no workstation confusion.
- Added block-recovery pages for hand-breakable Axle, Nest and Torches (including vanilla Torch families), with a note that Nest contents are returned. The Axle is infrastructure only, not a mechanical power provider.
- Updated block-drop pages to **exclude all Deepslate** (including Deepslate Coal Ore) and reflect exact Coal Hammer yield, Stone-family tool timings, and the one Raw Iron/Raw Copper Hammer drops. No new block-breaking behavior is introduced in PF-EMI.
- Added non-crafting Underground Hazards guides for Speluncaphobia and Claustrophobia, including their Y thresholds and artificial-light/daylight exemptions.
- Preserved JEI compatibility work from pf.16 and source-only patch protocol. Static tests and ZIP validation do not equal a full Gradle or Minecraft runtime test.

## 1.1.23-pf.16 — JEI compatibility warning and fluid-amount fix (2026-11-01)

- Keep required legacy JEI interface implementations for compatibility while routing their implementation through the current `add(...)` APIs. Apply **method-scoped** `@SuppressWarnings("removal")` only where the current JEI interface still demands an implementation of a deprecated-for-removal method. This hides known compatibility warnings; it does **not** make the deprecated JEI contracts future-proof.
- Fix a real fluid-ingredient bug: `JemiRecipeSlotBuilder.add(Fluid, long)` previously discarded the requested amount and used one bucket; it now forwards the actual amount.
- Restrict the unchecked self-cast suppression in `JemiPlaceable` to its `setPosition` method, removing one generic unchecked compiler note without broad Gradle lint suppression.
- Preserve ProjectFlint **0.31.52-b** recipe integration and all pf.15 gameplay-facing behavior; no recipe, multiblock, or loot changes.
- **Verification:** local Java syntax parsing and patch integrity only. The user's pasted log had no errors in the shown compilation stages but did not include `BUILD SUCCESSFUL` or `BUILD FAILED`. Full Gradle / runtime verification must be done on a system with dependencies available.

## 1.1.23-pf.15 — ProjectFlint 0.31.52-b Hardwood recipe-type integration (2026-11-01)

- Target ProjectFlint **0.31.52-b** at GitHub commit `37c5f2af2a9939ac71c9d04664954caf10beeb7c`. v0.31.52-b is the RecipeDisplay import fix; the substantial mechanics and new recipe system came in 0.31.51-b.
- **Read the new `projectflint:hardwood_crafting` recipe type at runtime.** Discover `hardwood_shaped`, `hardwood_shapeless`, and `hardwood_durable` wrappers, preserve each shaped 3x3 grid and shapeless ingredient alternatives, and use the real result stack. This replaces the previous incorrect six-recipe hand-maintained Hardwood tool list, and automatically includes Hardwood Sticks, beams, masonry, books, paper, armor, dyes, doors, staged Rope, new tools and other registered station-only recipes.
- **Handle unsynchronized/custom-server recipe data:** if the new recipe type supplies no client entries, read actual station-only JSONs from the installed ProjectFlint Fabric mod's `data/projectflint/recipe` resources; do not invent a vanilla crafting recipe or require a compile-time ProjectFlint dependency. Server-synchronized recipes take priority over this bundled fallback. In the fallback case, custom datapack removals/overrides may not be reflected and must be verified in-game.
- **Remove retired recipe views:** legacy personal 2x2 wooden axe/pickaxe/shovel, hinged doors, and the incorrectly personal-craftable Hardwood Table. The replacement workstation-only JSONs belong in the Hardwood 3x3 category. Keep real personal-grid recipes (including wood processing and Fat Torch) in their correct category.
- Update block-drop guidance to ProjectFlint's **OreMiningRules**: 13.5-second wooden-tier Stone/Coal breaking, Hardwood Hammer's 2-Coal yield, ordinary Iron/Copper access for the Hammer, no extra deepslate Iron/Copper Hammer tier, and 45-second unqualified ore breaking without drops. Correct the old `projectflint:wooden_pickaxe` lookup to vanilla `minecraft:wooden_pickaxe`.
- Display reusable tools and forms as catalysts for Hardwood durable recipes; show Clay Bucket and Unfired Brick Mold remainders instead of presenting them as permanently consumed.
- ProjectFlint item components remain authoritative for the new Hardwood durability values (**320 uses**, Hammer **560**). The Hardwood Table's 3-second per-craft delay remains visible in the 3x3 EMI pages.
- Updated release script and Fabric build names. Static verification only; a full Minecraft build and runtime validation are not claimed.

## 1.1.23-pf.14 — UI readability, rail interactions and ProjectFlint 0.31.50-b sync (2026-10-29)

- Fixed overflowing and low-contrast world/loot guidance: bounded word wrapping, content-driven height, dark text for the default light EMI recipe theme.
- Removed the extraneous shapeless badge overlapping the result area in custom 2×2 and 3×3 recipes.
- Made the multiblock **IN/OUT rails actual EMI item slots**, preserving alternate material variants for recipe/use lookups. Removed the full-screen widget's tooltip interception, which prevented A/U navigation, without altering the 3D scene.
- Displayed **3 seconds (60 ticks) per craft** directly on the Hardwood 3×3 recipe pages rather than only in a hover tooltip.
- Synced ProjectFlint **v0.31.50-b** (GitHub commit `169797e5440474457fd3410456bf43401214bf3c`): corrected Fat-Burning Torch (Stick + String + Charcoal, no Fat until placed), Hardwood Crafting Table (two Hardwood Alloy + Cobblestone + **Bound Wood Processor**), and all nine wood-family Log / Stripped Log plank transformations accepting either Wooden Axe or Hardwood Axe.
- Added automatic category routing for newly registered *ordinary ProjectFlint* shaped/shapeless crafting recipes missed by the static list: true ≤2×2 recipes go to Personal Crafting and larger recipes go to Hardwood Crafting. Native non-crafting recipes are preserved; native crafting duplicates are hidden when mirrored. The Wooden Grinder-exclusive **Tanned Utility Vest** recipe is not displayed as generic crafting.
- Documented **Clay Bucket → Clay Milk Bucket** from right-clicking a living adult Cow in the mob-interaction section, and filling/emptying water using Clay Buckets in world interactions. The milking process is explicitly **not a kill drop**.
- Updated the source-only verification checklist. Local source assertions and ZIP validation are not a substitute for compiling/running Minecraft. **Only the Fabric target in this source archive has been patched; Forge/NeoForge parity is not claimed.**

## 1.1.23-pf.13 — Recipe sorting and missing integration coverage (2026-10-28)

- **Removed the erroneous `recent_world_processing` catch-all category.** Basin jumps, Fireclay Forge operations, strawberry planting/harvesting, and Fat-Burning Torch refueling are now displayed under meaningful, separate categories.
- Added dedicated **Personal Crafting (2×2)** and **Hardwood Crafting Table (3×3)** recipe pages, including nine wood families, portable leatherworking, tool recipes, Fat-Burning Torch production, Hardwood material and table production, and six exact-shaped Hardwood tools. Generic native Crafting entries are selectively remapped only for those source-verified recipe IDs.
- Added the missing **25 Sanding Station flower/ink dye** conversions and **Wooden Grinder inheritance** of Clay Grinder conversions. Added the missing Pumpkin Cake slicing and ProjectFlint Brush upgrade entries.
- Added **Primitive Oven direct food/cooking** recipes in their actual oven category, and Brick Furnace-exclusive **Tanned Leather and Glass** production. These exclusives are deliberately omitted from Brick Blast Furnace.
- Corrected the **empty Clay Bucket return** for Wooden Grinder recipes that use filled Clay Buckets.
- Added English category translations, a true grid-geometry display recipe, category-by-category audit documentation, and a pf.13 static validation script.
- **Not certified as release-ready:** all 30 local source-level checks pass, but no full Gradle compilation or in-game GUI/runtime test was possible. This is a source patch for testing; the global parity checklist remains open.

## 1.1.23-pf.12 — Source-parity corrections and loot lookup (2026-10-28)

- Corrected Bookshelf research outputs to **actual bound Recipe Parchments**, with the `ProjectFlintRecipe` custom-data component and matching unfinished-bacon model override. Added the staged Bacon & Eggs research step, including its required unfinished parchment; corrected 4-book eligibility indicators for the missing pages.
- Corrected all registered Primitive Oven parchment catalysts to the exact bound recipe ID, including efficient Fat, breads, jams, cheese, sushi, and dishes. These remain display-only EMI recipes and do not change game logic.
- Added ten source-derived Clay Grinder transformations (tanned armor, fireclay shapes, unfired-brick recycling, Loose Dirt slabs). Added Wooden Grinder's woven-frame Clay Bucket alternate and 512-use Leather Armor variants.
- Brought permanent campfire heat entries into alignment with ProjectFlint's `PrimitiveProcessingRegistry`: Burnt Twig/Branch, firing Unfired Clay Buckets, Unfired Bricks, and Unfired Fireclay Bricks. Avoided duplicating kiln-only entries.
- Extended Fireclay Forge log/wood accepted-input examples to stripped and bark variants; verified the six-layer Forge and eight-layer Well structure definitions against ProjectFlint world validators.
- Turtle loot pages now distinguish its guaranteed one adult meat drop from the alternative cooked-on-fire drop. Mob loot spawn eggs are **non-consumed catalyst lookup keys**, making these pages discoverable under Uses; they are not survival crafting ingredients.
- Reduced the minimum multiblock rendering scale to fit unusually tall models in the recipe viewport. Updated source version/build launcher/patch applicator.
- Expanded the version-pinned audit and added a local static regression script. The complete multi-loader/runtime checklist is **not** completed: builds, GUI tests, loot conditions, and the entire ProjectFlint source tree were not available in this environment. No binary/JAR compatibility is claimed.

## 1.1.23-pf.11 — ProjectFlint v0.31.48-b compatibility audit (source patch)

**Structure viewer**
- Increased the structure recipe canvas from **300 × 240** to **312 × 276**. The model is centered and fit-scaled within its own clipped viewport, with separate material/output rails and a bottom row for rotation, layer navigation and details.
- Adjusted long title/dimension rendering to prevent collisions and allowed smaller structures to fit without forced oversizing.
- Added a **3 × 3 × 6 Fireclay Forge** guide: solid 3×3 foundation; controller ring; four corner stairs with cardinal brick walls; two open upper cardinal rings; four single-slab caps; open chimney. Material counts: **28 Fireclay Brick Blocks, 4 Fireclay Brick Stairs, 4 Fireclay Brick Slabs, 1 controller**.

**New recipe and interaction documentation**
- Updated **Wooden Grinder** routes for the Chicken Nest, Well Pulley, Tanned Utility Vest, Clay Bucket variants, reusable Brick Mold, fired bricks, Fireclay, Brick Furnace, Fireclay Forge, Wheat Seeds, Flour, milk cups, Fat and brick conversions.
- Added **Bookshelf research** and **Primitive Oven** displays for strawberry jam/pie, Dough/Sweet Dough, Bread, specialty breads, efficient Fat, baked eggs, sushi and cheese. Where bound parchment metadata is required, the EMI route is currently illustrative and does not reproduce the item component in its output.
- Added informative **Basin** Tannin/Wood Dust, **Strawberry** planting/harvesting, **Fat-Burning Torch** refueling, **Hardwood** material/tool-station progression, and **Fireclay Forge** Charcoal/Hardwood Alloy cycles.
- Added **Unfired Brick → vanilla Brick** and **Unfired Fireclay Brick → Fireclay Brick** heat-guide entries, Turtle meat cooking and a qualified Wooden Pickaxe **Coal Ore → Coal** block-drop page.
- Retained the existing **Sleeping Bag placement**, **world interactions**, **block breaking/recovery**, and **mob loot** recipe categories, which still require per-case parity testing. Newly registered ProjectFlint items are no longer hidden solely for lacking a hardcoded EMI recipe.

**Maintenance**
- Replaced a `build.bat` dependency on an absent APPLY script with a standalone Java-21 Fabric build launcher; preserves the Windows wrapper and remapped Fabric JAR output handling.
- Removed the separate historic PF release-note files and consolidated history here. Gradle now excludes a missing NeoForge project from this Fabric-targeted snapshot.
- Added a detailed, per-item verification checklist and `tools/audit_projectflint_emi.py` to compare against a complete local ProjectFlint source checkout.

**Verification:** Source changes and reference checks performed against ProjectFlint `main` at commit [`6f45b748`](https://github.com/bostonstuddard/ProjectFlint/commit/6f45b748eb808e50f9945ee174f9ccf255362862) (`0.31.48-b`). **Compilation and runtime validation are NOT complete**; the Gradle wrapper could not fetch Gradle 9.2.1 here. This patch must not be called a finished 100%-accurate release.

## 1.1.23-pf.10 — World, drop and multi-block documentation

- Added a 300 × 240 isometric multiblock viewer with separate ingredient/output areas, rotation, layer navigation and hover information.
- Added world-placement routes for the supported two-block Sleeping Bag, clay assembly, water vessels, bone brushing, vegetation and soil interactions.
- Added an initial ProjectFlint-specific **Block Drops/Recovery** category for tool-dependent harvesting and dismantling, and **Mob Loot** pages for selected ProjectFlint-modified animal drops.
- Used spawn eggs as *visual entity identifiers* rather than obtainable survival crafting inputs.

## 1.1.23-pf.9 — Multiblock interface layout

- Separated the 3D preview, layer controls and material/output text to prevent the earlier Well preview's controls, legends and materials from overlapping.

## 1.1.23-pf.8 — Source-backed mechanics correction

- Corrected in-world assemblies for Flint Axe, Flint Plant Cutters, Advanced Flint Multitool, Crude Multitool and Lumber Processor output variants after inspecting ProjectFlint gameplay code.
- Reconciled Well casing and frame counts with the then-available validator, and corrected Well outputs.
- Consolidated ProjectFlint recipe and multiblock registration into one plugin entrypoint to avoid registration-order omissions.

## 1.1.23-pf.7 — Initial 3D structure guides

- Replaced flat structure layers with a depth-ordered isometric renderer using Minecraft item/block models; enabled rotation and cutaway layers.
- Fixed the Well's earlier hollow-vs-solid foundation mistake and exposed production outputs. Later source audit in pf.8 superseded pf.7's brick count; historical quantities should not be used as installation instructions.

## 1.1.23-pf.6 — Mixin runtime repair

- Removed an invalid non-shadow static field from `RecipeScreenMixin` that caused an `InvalidMixinException` on Fabric startup; retained recipe-page boundary fixes.

## 1.1.23-pf.5 — Gradle memory and workers

- Restored explicit memory limits: Gradle heap **4 GiB**, metaspace **1 GiB**, two workers and UTF-8 encoding to prevent remapping crashes caused by default low heap.

## 1.1.23-pf.3–pf.4 — Minecraft 1.21.11 toolchain migration

- Updated Architectury Loom 1.13 → 1.14, Gradle 8.14 → 9.2.1 and Shadow 8.3.6 → 9.6.1; maintained Java 21.
- Adapted the build/patch configuration during 1.21.11 migration.

## 1.1.23-pf.2 and earlier — Original compatibility work

- Began hardcoded ProjectFlint world crafting and processing guides (knapping, ground assemblies, cane, clay, wood, water washing and fire processing).
- Improved EMI's survival item visibility filters and narrow-screen workstation margins; technical/admin-only vanilla items remained excluded from survival listing.

---

### Development and release policy

- ProjectFlint owns all actual recipe/loot/interaction logic. PF-EMI is a **viewer and compatibility layer**, not a source of new gameplay recipes.
- Exact ingredient variants, chances, durability consumption, research parchment components and loot conditions must match the current *source implementation*, not an old changelog entry.
- A checklist item is complete only when it has been compared to the pinned gameplay source **and**, where applicable, exercised in-game. Keep incomplete items unchecked.
- Archived versions' installer names and historical workaround scripts are intentionally omitted. `build.bat` in the current source tree is the only intended direct build entrypoint.
