# PF-EMI × ProjectFlint — Exhaustive Audit Checklist

Target: **ProjectFlint v0.31.48-b** (`bostonstuddard/ProjectFlint` commit `6f45b748eb808e50f9945ee174f9ccf255362862`, verified 2026-10-28); PF-EMI **1.1.23-pf.12**, MC **1.21.11**.

**Definition of checkmarks:** `[x]` means the specific source fact, source edit, or archive existence was checked here. It does **NOT** mean a full runtime regression, nor imply complete equivalence. `[ ]` is intentionally incomplete or requires live testing/full source review.

## Audit result and blockers

- **Source parity and runtime audit remain incomplete.** The pf.12 patch makes additional directly compared fixes. Hundreds of per-item checks still need an exhaustive ProjectFlint tree and live game testing. This is a precise continuation checklist rather than a certification.
- ProjectFlint Github source was inspected through connected GitHub file reads, but a full local clone was unavailable due to external networking restrictions.
- Gradle wrapper 9.2.1 cannot download through this sandbox. No successful compilation, gameplay test or screenshot verification was obtained.
- The build archive has Forge-era sources but only Fabric is claimed for this targeted pf.12 build; do not assume current Forge/NeoForge integration is working.

## pf.12 acceptance pass — newly resolved source discrepancies

**Static/source-reviewed, not gameplay-tested:**

- [x] Read the current GitHub `RecipeParchmentItem.java` and reproduced `CUSTOM_DATA.ProjectFlintRecipe` on all researched/used parchments; matched `CustomModelData` for the unfinished Bacon & Eggs parchment.
- [x] Checked `BookshelfRecipeRegistry.java` stage and four-book gate; added the intermediate-to-final Bacon & Eggs research step.
- [x] Checked `PrimitiveStoveRecipeRegistry.java` listed oven processes and matching parchment IDs; retained its 300-tick cook time.
- [x] Checked `ClayGrinderRecipeGuideRegistry.java` and added tanned leather armor, fireclay shapes, Unfired Brick recycle, Loose Dirt slab consolidation.
- [x] Checked `WoodenGrinderRecipeRegistry.java` for custom-max-damage leather armor and alternate Clay Bucket forming recipe.
- [x] Checked `PrimitiveProcessingRegistry.java` for BOTH campfire/kiln eligible brick, fireclay, bucket and twig/branch heating.
- [x] Checked `PrimitiveAnimalDropSystem.java` for exactly one Turtle meat (cooked on fire); registered a non-consumed mob-identity lookup.
- [x] Checked `WellMultiblock.java` and `FireclayForgeMultiblock.java` source constraints for the displayed frame/ring model and counts.
- [x] Added and executed `tools/verify_pf_emi_pf12.py`: 20/20 static assertions pass; compile/runtime remain untested.
- [ ] Obtain a full exact local ProjectFlint `6f45b748` checkout; all JAVA/JSON tags, generated recipes, datapacks, LootTables and Mixins must be covered.
- [ ] Compile and load PF-EMI pf.12 alongside target ProjectFlint and check complete GUI and recipes in-game.
- [ ] Validate every block and mob loot condition in survival/creative with tool/difficulty/state variants.
- [ ] Confirm compatibility with Forge and NeoForge independently before deployment.

**Marking rules:** `[x]` above documents a verified source comparison or completed patch/test asset. It does not certify that a Minecraft client has successfully executed the route.

## Source and testing workstreams

### Source snapshot / scope

- [x] Identified latest GitHub ProjectFlint gradle.properties: 0.31.48-b, Minecraft 1.21.11
- [x] Recorded GitHub source revision 6f45b748eb808e50f9945ee174f9ccf255362862 (2026-10-28)
- [x] Confirmed uploaded PF-EMI snapshot is 1.1.23-pf.10 and source targets 1.21.11
- [ ] Download current ProjectFlint GitHub tree as an exact local snapshot; compare every Java/JSON resource
- [x] Checked newest GitHub commits again on 2026-10-28; still pinned at 6f45b748
- [ ] Compile against both current PF and PF-EMI dependency graphs
- [ ] Test a development world in both fresh and upgraded saves

### Structure viewer UI / model correctness

- [x] Source change: canvas increased to 312 × 276 and oversized minimum model scale removed
- [x] Source change: header/dimensions collision handled; existing scissor and rail separations retained
- [x] Source-derived Fireclay Forge 3×3×6 casing pattern added to multiblock registry
- [ ] Launch game, verify full 3D block models rather than flat-looking item icons
- [ ] Test 3:2, 4:3, 16:9 and 21:9 and GUI scale 1–4 for clipping/overflow
- [ ] Rotate each structure through 360° and verify layer visual ordering
- [ ] Verify controller placement highlight; hover tooltips at all rotations
- [ ] Test non-solid blocks, translucent air/support markers and tall Well fit
- [ ] Verify exact counts, dimensions and patterns for all multiblocks against live world validators

### New source-confirmed 0.31.3x–0.31.48 feature representations

- [x] Source compared: FireclayForgeMultiblock validation layout and materials
- [x] Source compared: FireclayForgeBlockEntity 4-log/60-second charcoal fuel, 5-minute charcoal/Hardwood processing
- [x] Source compared: WoodenGrinderRecipeRegistry additions: Nest, Well Pulley, Brick Furnace, Fireclay Forge and Fireclay Brick input/output
- [x] Source compared: WoodenGrinderRecipeRegistry additions: wheat, flour, raw-meat fat, milk cup, reusable Brick Mold, Vest
- [x] Source compared: PrimitiveStoveRecipeRegistry strawberry, dough, ten breads, fat, sushi, cheese and egg quantities
- [x] Source compared: BookshelfRecipeRegistry latest research list and four-book requirement
- [x] Source compared: BasinBlockEntity 1/2 item processing, one hunger point, wood/plank inputs
- [x] Source-derived v0.31.45–48 Hardwood/Basin/Pickaxe/Torch guidance from ProjectFlint CHANGELOG.md
- [x] Replaced generic parchment output/catalyst stacks with exact custom-data-bound Recipe Parchment stacks; source implementation compared in `RecipeParchmentItem.java`
- [ ] Compare full signed player 2×2 and Hardwood Table 3×3 recipes and native recipe imports
- [ ] Compare all Cloth/Tanned Leather, Fireclay and Hardwood tool crafting and repair recipes
- [ ] Verify all categories do not duplicate base ProjectFlint/native recipes at runtime
- [ ] Check all early-game balance changes, probability and stage conditions against source logic

### World interaction and world-dependent production

- [x] Woven Wool (8) places a supported two-block Sleeping Bag in existing PF-EMI
- [x] Clay Grinding Base from eight Super Heated Clay represented by existing PF-EMI
- [x] Bone Brush sand/gravel and sheep yields represented by existing PF-EMI
- [x] Strawberry planting, harvesting, and Basin world process entries added
- [x] Wood/charcoal Fireclay Forge fuel process and Hardwood Alloy processing added
- [ ] Reverify physical crafting all items and assemblers (input order, offhand, catalysts)
- [ ] Verify loot with/without Tanned Utility Vest overrides
- [ ] Verify plant states, support loss, block state restrictions and tool durability
- [ ] Verify Well aquifer and quality roll calculations incl GUI rope 3n+floor(n²/4)
- [ ] Check firestarter consumption, fuel timing and reclaim/drop behavior
- [ ] Map all other clicked-entity/clicked-block events to EMI

### Block breaking and mob loot

- [x] Custom block drops / recovery category present in supplied pf.10 source
- [x] Source-based Wooden Pickaxe Coal Ore -> Coal route added
- [x] Selected ProjectFlint mob drop category present in supplied pf.10 source
- [x] Turtle custom meat page added; actual count deliberately not certified
- [ ] Parse server-side block drops and loot tables for ALL ProjectFlint and affected vanilla blocks
- [ ] Check every tool tier, blockstate, silk touch, Fortune, explosions, support loss and placement provenance
- [ ] Check every entity drop including looting/age/fire/tamed conditions, zero-drop cases and probabilities
- [ ] Add base Minecraft loot-table coverage only when no conflict with ProjectFlint custom systems
- [ ] Ensure no removed/unobtainable drop is shown as guaranteed
- [ ] Double-check mob spawn-egg reference visibility independent of normal EMI index

### Build, packaging and changelog

- [x] Changed source version to pf.12
- [x] Consolidated nine scattered release notes into one CHANGELOG.md; deleted stale duplicates
- [x] Removed dangling APPLY script call in build.bat and disabled absent NeoForge Gradle project
- [x] Excluded generated Gradle/build/git data from planned source distribution
- [ ] Gradle :fabric:build success on Java 21
- [ ] Real client launch + world load (new and existing worlds)
- [ ] EMI startup logs contain no exceptions/warnings caused by PF-EMI
- [ ] Confirm recipes exist and do not disappear after resource reload / multiplayer join
- [ ] Check Forge/NeoForge compatibility separately before claiming multi-loader support

## Specific multiblock definitions and test passes

### `lumber_processor`

- [x] A static definition exists in `ProjectFlintMultiblockPlugin.java`.
- [ ] Compare every cell to the authoritative validator, including orientation and state predicates.
- [ ] Compare required material quantities to actual placement and formation costs.
- [ ] View on-screen with rotation and all/layer-by-layer; no overlap or scaling defect.
- [ ] Confirm output details, fuel/byproduct flags, workstation category, and recipe links.

### `well`

- [x] A static definition exists in `ProjectFlintMultiblockPlugin.java`.
- [ ] Compare every cell to the authoritative validator, including orientation and state predicates.
- [ ] Compare required material quantities to actual placement and formation costs.
- [ ] View on-screen with rotation and all/layer-by-layer; no overlap or scaling defect.
- [ ] Confirm output details, fuel/byproduct flags, workstation category, and recipe links.

### `primitive_campfire`

- [x] A static definition exists in `ProjectFlintMultiblockPlugin.java`.
- [ ] Compare every cell to the authoritative validator, including orientation and state predicates.
- [ ] Compare required material quantities to actual placement and formation costs.
- [ ] View on-screen with rotation and all/layer-by-layer; no overlap or scaling defect.
- [ ] Confirm output details, fuel/byproduct flags, workstation category, and recipe links.

### `baked_kiln`

- [x] A static definition exists in `ProjectFlintMultiblockPlugin.java`.
- [ ] Compare every cell to the authoritative validator, including orientation and state predicates.
- [ ] Compare required material quantities to actual placement and formation costs.
- [ ] View on-screen with rotation and all/layer-by-layer; no overlap or scaling defect.
- [ ] Confirm output details, fuel/byproduct flags, workstation category, and recipe links.

### `mortar_kiln`

- [x] A static definition exists in `ProjectFlintMultiblockPlugin.java`.
- [ ] Compare every cell to the authoritative validator, including orientation and state predicates.
- [ ] Compare required material quantities to actual placement and formation costs.
- [ ] View on-screen with rotation and all/layer-by-layer; no overlap or scaling defect.
- [ ] Confirm output details, fuel/byproduct flags, workstation category, and recipe links.

### `advanced_kiln`

- [x] A static definition exists in `ProjectFlintMultiblockPlugin.java`.
- [ ] Compare every cell to the authoritative validator, including orientation and state predicates.
- [ ] Compare required material quantities to actual placement and formation costs.
- [ ] View on-screen with rotation and all/layer-by-layer; no overlap or scaling defect.
- [ ] Confirm output details, fuel/byproduct flags, workstation category, and recipe links.

### `fireclay_forge`

- [x] A static definition exists in `ProjectFlintMultiblockPlugin.java`.
- [ ] Compare every cell to the authoritative validator, including orientation and state predicates.
- [ ] Compare required material quantities to actual placement and formation costs.
- [ ] View on-screen with rotation and all/layer-by-layer; no overlap or scaling defect.
- [ ] Confirm output details, fuel/byproduct flags, workstation category, and recipe links.

## Static PF-EMI recipe ID inventory (each requires individual comparison)

Each checkbox below is intentionally **unchecked**: a present registration call does not prove the quantities/conditions are correct. Dynamic loops are not fully expanded here; they require a separate check.

### world (57 statically named registration calls)

- [ ] `rock_to_refined` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `rock_to_very_refined` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `crude_plant_string` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `crude_stone_hammer` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `plant_tool_binding` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `crude_plant_cutters` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `crude_multitool` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `flint_firestarter` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `flint_multitool` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `flint_axe` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `flint_plant_cutters` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `advanced_flint_multitool` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `reed_backpack` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bone_brush` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `crudely_woven_bowl` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `loose_dirt` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `crude_clay_block_from_fired_clay` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay_grinder_assembly` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `campfire_stone` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `split_burnt_twig` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `knap_flint` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `reed_roll` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand_ball` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `reeds_cutters` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `reeds_crude_multitool` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `reeds_flint_multitool` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `reeds_advanced_multitool` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wattle` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `tied_wattle` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `unfired_wattle_and_daub` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `filed_bone` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bone_firestarter` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `woven_wool` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `black_dye_from_ink` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `woven_fan` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `mud_pile` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wet_daub_pile` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `washed_leather` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sugarcane_roll` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `unfired_mortar_bricks` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `daub_pile` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sanded_wood_slab_world` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `woven_wood_slab_world` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `mortar_kiln_formation` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wooden_grinder_formation` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `woven_chest_upgrade` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `loose_dirt_slab` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `unfired_woven_bowl` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `mortar_chest` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `fired_clay_bowl_2` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `fired_clay_bowl_4` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `fired_clay_bowl_6` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `unfired_clay_ball_from_bowl` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay_pot_2` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `unfired_clay_ball_from_pot` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `crude_clay_kiln` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `lumber_processor_formation` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.

### worldTimed (1 statically named registration calls)

- [ ] `fired_clay` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.

### simple (99 statically named registration calls)

- [ ] `clay/bone_club` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/bone_helmet` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/bone_chestplate` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/bone_leggings` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/bone_boots` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/clay_mortar` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/flint_chips` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/flint_from_chips` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/grinded_wood` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/sanded_reverse` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/stone_grindsaw` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/crushed_stone` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/crushed_slab` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/crushed_block` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/mortar_slab` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/mortar_block` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/mortar_kiln_base` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/sanding_paper` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/twig_to_branch` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/branch_to_twigs` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/unfired_kiln_glass` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/floor_tile` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/wooden_grinder_base` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/woven_wood` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/woven_slab` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/loose_mud_slab` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/loose_mud_block` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/crude_clay_slab` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/crude_clay_block` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/fired_clay` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/burnt_branch_block` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/burnt_branch_slabs` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/burnt_branches` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/unfired_` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/bamboo_slab` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/bamboo_block` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/ground_leather` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/woven_chest_frame` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/leather_backpack` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/backpack_chest` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/wattle_window` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/woven_fan` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/sanding_paper` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/tied_wattle` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/daub` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/unfired_wattle_daub` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/torch_holder` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/unlit_flint_torch` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/flint_torch_holder` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/sanding_station` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/water_bearing_pot` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/water_pot` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/bound_wood_processor` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/wood_firestarter` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/bone_hoe` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/bookshelf` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/flint_file` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/brick_shingles` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/door_hinge` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/chest_frame` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/wattle_door` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/loose_mud_bricks` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/bricks` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/primitive_stove` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/bone_knife` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/rope_stage_1` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/rope_stage_2` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/rope_stage_3` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/nest` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/well_pulley` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/brick_furnace` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/fireclay_forge_controller` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/unfired_fireclay_brick` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/wheat_seeds` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/flour` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/fat` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/milk_cups` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/utility_vest` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/vanilla_bricks` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/wattled_daub_from_bricks` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/wooden_cabinet` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/woven_fan` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/advanced_kiln_glass` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/leather` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/filed_bone` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/wood_slab` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/woven_wool` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/sand_ball` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/flint_file_head` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/twigs` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/cobweb` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/smooth_limestone` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/polished_limestone` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/glazed_tile` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/checkered_tile` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sand/recipe_parchment` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bookshelf/book` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay/` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.

### process (39 statically named registration calls)

- [ ] `wood/aquifer_finder` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/wooden_sheet` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/wooden_screw` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/limestone_bricks` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/chiseled_limestone` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/wooden_cup` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/rope` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/unfired_brick_mold` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/unfired_bricks` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wood/unfired_clay_bucket_bone_knife` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bookshelf/writable_book` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bookshelf/research/` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bookshelf/research/pumpkin_cake` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bookshelf/research/pumpkin_pie` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bookshelf/research/butter` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bookshelf/research/modern/` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bookshelf/research/modern/cheese` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bookshelf/research/bone_broth` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `lumber/boards` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `lumber/beams` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `well/cycle` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `heat/crude/bake` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `advanced/burnt_branch_block` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `advanced/crude_clay_block` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `advanced/` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `placement/sleeping_bag` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `placement/clay_grinding_base` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `placement/fill_water_bearing_pot` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `placement/fill_water_pot` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `placement/brush_sand` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `placement/brush_gravel` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `basin/tannin` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `basin/wood_dust` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `forge/wood_to_charcoal` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `forge/hardwood_alloy` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `hardwood/material` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `hardwood/table_assembly` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `stove/` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `heat/` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.

### stoveRecipe (31 statically named registration calls)

- [ ] `sugar` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sweet_berry_jam` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sweet_berry_pie` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `pumpkin_pie` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `melon_pie` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `mutton_pie` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `butter` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `pumpkin_cake` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `apples_n_pie` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `beef_stew` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bacon_and_eggs` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `strawberry_jam` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `strawberry_pie` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `fat_efficiency` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `dough` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sweet_dough` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bread` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `rustic_bread` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sweet_bread` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `flatbread` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `french_bread` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `meat_bread` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `cheese_bread` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `egg_bread` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `breakfast_bread` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `apple_bread` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `pumpkin_bread` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `scrambled_egg_oven` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sushi_roll` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `cheese` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bone_broth` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.

### info (8 statically named registration calls)

- [ ] `placement/sweet_berry_bush` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `placement/bone_hoe_farmland` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `placement/bone_brush_sheep` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `placement/strawberry_seeds` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `placement/strawberry_harvest` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `torch/fat_burning_torch` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `drops/` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `mob/` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.

### blockDrop (42 statically named registration calls)

- [ ] `coal_ore_wooden_pickaxe` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sleeping_bag_salvage` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sleeping_bag_full` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `beam/` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `crushed_stone_block` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `loose_mud_bricks` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `loose_mud_brick_slab` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `limestone/` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sanding_station` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `bookshelf` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `woven_chest` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wooden_grinder` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `mortar_kiln` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `mortar_kiln_base` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wooden_grinder_base` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `advanced_kiln` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `wattle_floor_tile` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `mortar_bricks` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `mortar_brick_slab` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `crude_clay_block` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `crushed_stone_slab` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `crude_clay_slab` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `baked_kiln_full` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `baked_kiln_salvage` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay_grinder_full` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `clay_grinder_salvage` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `stone_hammer` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `gravel_crude` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `gravel_flint` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `gravel_advanced` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `short_grass_binding` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `grass_string` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `leaf_litter` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `grass_scrape` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `dirt_crude` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `dirt_flint` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `dirt_advanced` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `pumpkin_slice` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `melon_slice` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `pumpkin_seeds` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `melon_seeds` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `strip/` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.

### mob (16 statically named registration calls)

- [ ] `chicken` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `pig` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `sheep` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `cow` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `squid` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `horse` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `fox` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `frog` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `goat` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `llama` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `dolphin` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `polar_bear` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `black_bear` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `brown_bear` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `turtle` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.
- [ ] `camel` — inspect source gameplay recipe and EMI inputs, outputs, count, reusable items, chance, time, conditions and tooltip.

## Referenced custom ProjectFlint items (resource-by-resource checklist)

A reference in the PF-EMI plugin is **not proof of a valid or correct recipe**, and does not include source items formed by dynamically constructed identifiers.

- [ ] `projectflint:advanced_flint_multitool` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:advanced_kiln_glass` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:animal_bones` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:apple_bread` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:apples_n_pie` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:aquifer_finder` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bacon_and_eggs` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:baked_clay_kiln` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:baked_kiln_top` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:basin` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:beef_stew` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:black_bear_spawn_egg` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bone_boots` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bone_broth` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bone_brush` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bone_chestplate` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bone_club` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bone_firestarter` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bone_helmet` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bone_hoe` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bone_knife` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bone_leggings` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bookshelf` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:bound_wood_processor` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:branch` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:breakfast_bread` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:brick_furnace` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:brick_shingles` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:brown_bear_spawn_egg` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:burnt_branch` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:burnt_branch_block` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:burnt_branch_slab` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:burnt_twig` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:butter` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:campfire_stone` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:checkered_tile` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:cheese` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:cheese_bread` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:chiseled_limestone` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:clay_grinder` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:clay_grinding_base` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:clay_milk_bucket` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:clay_mortar` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:clay_pot` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:clay_pot_2` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:clay_water_bucket` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:cobbled_limestone` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:cobweb` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:crude_clay_block` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:crude_clay_kiln` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:crude_clay_slab` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:crude_harvesting_tool` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:crude_multitool` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:crude_plant_string` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:crude_reed` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:crude_stone_hammer` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:crudely_woven_bowl` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:crushed_stone` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:crushed_stone_slab` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:daub_pile` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:dirt_pile` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:dough` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:egg_bread` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:fat` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:fat_burning_torch` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:filed_bone` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:fireclay_brick` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:fireclay_brick_block` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:fireclay_forge_controller` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:fired_clay` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:fired_clay_` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:fired_clay_block_mold` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:fired_clay_bowl` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:fired_clay_bowl_2` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:fired_clay_bowl_4` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:fired_clay_bowl_6` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flatbread` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flax_seeds` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flint_axe` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flint_axe_pieces` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flint_chip` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flint_cutter_piece` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flint_file` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flint_file_head` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flint_firestarter` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flint_multitool` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flint_plant_cutters` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flint_torch_holder` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:flour` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:french_bread` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:glass_shard` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:glazed_checkered_tile` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:grinded_wood_slab` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:ground_leather` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:hardwood` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:hardwood_alloy` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:hardwood_crafting_table` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:leather_backpack` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:leather_backpack_with_chest` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:lightly_burnt_twig` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:limestone` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:limestone_bricks` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:loose_dirt` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:loose_dirt_slab` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:loose_mud_brick_slab` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:loose_mud_bricks` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:loose_soil` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:lumber_processor` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:meat_bread` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:melon_pie` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:mortar_brick_slab` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:mortar_bricks` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:mortar_chest` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:mortar_kiln` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:mortar_kiln_base` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:mud_pile` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:mutton_pie` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:nest` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:plant_fiber` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:plant_seeds` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:plant_tool_binding` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:polished_limestone` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:primitive_stove` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:pumpkin_bread` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:pumpkin_cake` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:pumpkin_slice` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:raw_bear_meat` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:raw_calamari` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:raw_camel_meat` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:raw_dolphin_meat` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:raw_fox_meat` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:raw_frog_meat` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:raw_goat_meat` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:raw_horse_meat` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:raw_leather` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:raw_llama_meat` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:raw_turtle_meat` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:recipe_parchment` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:reed_backpack` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:reed_roll` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:refined_flint_multitool_piece` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:refined_rock` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:rock` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:rope` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:rope_stage_1` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:rope_stage_2` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:rope_stage_3` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:rustic_bread` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sand_ball` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sand_flake` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sanded_wood_slab` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sanding_paper` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sanding_station` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:scrambled_egg` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sleeping_bag` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:smooth_limestone` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:stone_grindsaw` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:strawberry` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:strawberry_jam` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:strawberry_pie` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:strawberry_seeds` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sugarcane_roll` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:super_heated_clay` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sushi_roll` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sweet_berry_jam` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sweet_berry_pie` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sweet_berry_seeds` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sweet_bread` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:sweet_dough` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:tanned_leather_chestplate` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:tanned_utility_vest` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:tannin` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:tied_wattle` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:torch_holder` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:twig` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:unfired_brick` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:unfired_brick_mold` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:unfired_clay_` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:unfired_clay_ball` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:unfired_clay_bucket` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:unfired_clay_mold_base` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:unfired_fireclay_brick` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:unfired_kiln_glass` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:unfired_mortar_brick` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:unfired_wattle_and_daub` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:unfired_woven_bowl` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:unlit_flint_torch` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:very_refined_stone` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:water_bearing_pot` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:water_bearing_pot_filled` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:water_pot` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wattle` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wattled_daub_bricks` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wattled_daub_door` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wattled_daub_floor_tile` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wattled_daub_window` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:well_pulley` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wet_daub_pile` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wood_dust` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wood_firestarter` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_beam` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_board` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_cabinet` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_chest` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_chest_frame` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_cup` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_cup_raw_milk` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_door_hinge` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_grinder` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_grinder_base` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_pickaxe` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_screw` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wooden_sheet` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:wool` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:woven_chest` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:woven_chest_frame` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:woven_fan` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:woven_wood` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:woven_wood_slab` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.
- [ ] `projectflint:woven_wool` — check at least one truthful creation route, all important use routes, and all tool/loot/advancement interactions.

## Dynamic and source-owned systems to explicitly reconcile

- [ ] `WoodenGrinderRecipeRegistry.java`: all definitions and control flags (consume/damage/reusable, pair permutation).
- [ ] `ClayGrinderRecipeGuideRegistry.java` and conversion loops (including all wood species and slabs).
- [ ] `PrimitiveStoveRecipeRegistry.java`: parchment ID components, first-use unlock and food timers.
- [ ] `BookshelfRecipeRegistry.java`: four-book gate, staged parchment, all research variants.
- [ ] Personal 2×2 crafting tables and Hardwood 3×3 recipes: exact shape, result counts, tool damage.
- [ ] `PrimitiveProcessingRegistry.java`: heat stations, furnace-only outputs, kiln vs campfire eligibility.
- [ ] `ServerPlayerGameModeMixin.java`: break permissions and tool-specific drops across every block.
- [ ] `PrimitiveInteractionSystem.java`: every block/entity right-click and world action.
- [ ] `PrimitiveAnimalDropSystem.java`: every entity and drop condition.
- [ ] `WellMultiblock.java` and `WellPulleyBlockEntity.java`: structure, source water, output chance, rope boost.
- [ ] `FireclayForgeMultiblock.java` and `FireclayForgeBlockEntity.java`: all structure and fuel behaviors.
- [ ] `ModItems.java`, `ModBlocks.java`, all registry item IDs and dynamic runtime registered objects.
- [ ] Vanilla loot-table and natural world loot parity; avoid presenting incompatible vanilla drops.
- [ ] Registry tags, client data components, food stats, empty containers and returning vessels.
- [ ] New MC items introduced after upstream EMI snapshot: remove stale invisibility filters.
- [ ] Run `python tools/audit_projectflint_emi.py path/to/ProjectFlint` after obtaining newest local source.
- [ ] Post-test second independent audit with screenshots/logs and save the specific commit hash.

**Static inventory size:** 293 statically named registration calls, 220 unique literal custom item names, 7 explicit multiblock definitions. This does **not** measure total real gameplay coverage.
