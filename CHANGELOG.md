# PF-EMI — Unified Changelog

This is the single changelog for the ProjectFlint compatibility branch of EMI. Minecraft **1.21.11**, Fabric is the supported build target for the packaged `build.bat`. Entries describe **PF-EMI compatibility features**, not changes to ProjectFlint gameplay itself. For release readiness and exact gameplay parity, see [`docs/PROJECTFLINT_EMI_AUDIT_CHECKLIST.md`](docs/PROJECTFLINT_EMI_AUDIT_CHECKLIST.md).

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
