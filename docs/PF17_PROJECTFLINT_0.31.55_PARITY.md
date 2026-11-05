# PF-EMI pf.17 — ProjectFlint 0.31.55-b source-parity audit

**Reference repository:** https://github.com/bostonstuddard/ProjectFlint  
**Pinned commit:** [`54184ab574bc1999ad616d91518d4681365fca96`](https://github.com/bostonstuddard/ProjectFlint/commit/54184ab574bc1999ad616d91518d4681365fca96)  
**Base for the patch:** PF-EMI `1.1.23-pf.16`. **Minecraft:** `1.21.11`; **target loader:** Fabric.

## Source-derived changes and PF-EMI treatment

| Source authority | Source-defined behavior | PF-EMI integration | Source-static status |
|---|---|---|---|
| `docs/releases/0.31.53-b.md`, `FatBurningTorchBlock` | Wall-mounted Fat Torch facing north/east/south/west corrected; no new recipe | Existing fuel/ignite guide updated with wall-orientation note | Checked |
| `docs/releases/0.31.54-b.md`, `OreMiningRules` | Vanilla stone-family tool-gated; no generic Vest mining or salvage | Mining informational pages updated to 270-tick Pickaxe / 240-tick Hammer | Checked |
| `docs/releases/0.31.55-b.md`, `OreMiningRules` | Deepslate and all Deepslate ores unavailable in survival; Coal Hammer yields two, 240 ticks; Iron/Copper Hammer yields exactly one raw item, 270 ticks | Incorrect Deepslate Coal mining pages removed; locked category and updated source-derived drops | Checked |
| `BasinMaterialProcessingSystem`, `docs/BASIN_MATERIAL_YIELDS.md` | 10 material inputs and exact independent-byproduct chances | 10 Basin pages with separate chance-bearing output slots; 1 food-point cost and jump-to-process text | Checked |
| `BasinBlockEntity` | Planks -> one Wood Dust; stripped Oak/Birch -> two Tannin | Existing two Basin recipes preserved | Checked |
| `BrickFurnaceRecipeRegistry.STONE`, `BrickFurnaceBlockEntity` | Cobblestone -> Stone, 300 base ticks with 1.5× speed | Normal Brick Furnace only, **200 effective ticks** | Checked |
| `recipe/axle.json`, `AxleBlock`, `PlayerPlacedBlockLedger` | 8 Hardwood Sticks + Rope -> Axle; place with 3D axis orientation, hand recovery | Dedicated Hardwood source-driven recipe, world-placement and hand-recovery pages | Checked |
| `PlayerPlacedBlockLedger.isHandRecoverableDecoration`, `ServerPlayerGameModeMixin` | Hand recovers Fat/Flint/vanilla Torches, Nest and Axle in 5 ticks; Nest ejects stored eggs | Block Drops/Recovery information pages | Checked |
| `PrimitiveEnvironmentEffectSystem`, `ModEffects` | Speluncaphobia below Y=0, stacks 60s up to IV; Claustrophobia below Y=-40, -7% movement/-1 damage; direct sunlight or block light >=13 removes both | Environmental Hazards category with non-crafting explanatory entries | Checked |
| New `hardwood_*` and `axle.json`, `basin_alternative.json` | Dedicated Hardwood-shaped/shapeless/durable recipes, including new Hardwood Sticks tool ingredients, alternate Basin, stone-group slabs/stairs and 2-plank slab patterns | **Runtime type discovery + ProjectFlint installed-mod JSON fallback**, not a hand-maintained six-tool list | Checked architecture; runtime discovery needs test |
| `recipe/*_door_hinged.json`, `RecipeManagerMixin` | Early 3-Planks + 1 Door Hinge returns for nine wood species | Nine Personal 2×2 recipes restored, exact output | Checked |
| ProjectFlint textures/model assets | New human/vanilla-derived sprites | No asset copying into PF-EMI; references installed ProjectFlint registry items | Checked |

## Basin chance matrix

All bonus percentages represent **independent Bernoulli trials**. Guaranteed outputs and independent successes may occur together. For Andesite, Granite, Stone and Diorite, the 1-or-2 main output is **mutually exclusive** at 50% each. A single wash consumes one input item plus **one food point**; Stone Dust and Slag can yield no output.

| Input | Output chances |
|---|---|
| Raw Iron | 1 Iron Dust guaranteed; +1 Iron Dust 75%; +1 Slag 50% |
| Raw Copper | 1 Copper Dust guaranteed; +1 Copper Dust 83%; +1 Copper Dust 24%; +1 Nickel Dust 10%; +1 Silver Dust 2% |
| Andesite / Granite | 1 Fertilizer 50% **or** 2 Fertilizer 50% |
| Stone / Diorite | 1 Stone Dust 50% **or** 2 Stone Dust 50% |
| Stone Dust | +1 Slag 50% (else nothing) |
| Gravel | 1 Flint guaranteed; +1 Quartz Dust 20%; +1 Iron Dust 1% |
| Sand | 1 Potassium Dust guaranteed; +1 Mica Dust 5%; +1 Gold Dust 1% |
| Slag | +1 Iron Dust 33%, +1 Copper Dust 33%, +1 Silicon Dust 33% (independently; zero–three outputs) |

## Category ownership and regression checks

- **Hardwood Crafting Table**: Dedicated `projectflint:hardwood_crafting` type and `hardwood_shaped`, `hardwood_shapeless`, `hardwood_durable`; Axle, alternative Basin, Hardwood Sticks, slab/stair and tool recipes all belong here. Do not recreate these under generic 2×2 Crafting.
- **Personal Crafting**: Leatherworking, basic wood transformation, 2×2 Hinged Door repairs, material conversions. Retired wooden-tool 2×2 crafting entries must not return.
- **Basin**: Processing only; chance-bearing washes are not guaranteed crafting or item loot tables.
- **Brick Furnace**: Cobblestone -> Stone plus existing Tanned Leather/Glass exclusives; never duplicate as Primitive Kiln or Brick Blast Furnace exclusives.
- **Block Drops & Recovery**: Differentiates hand pickup from ore-break requirements; avoids stale Deepslate Coal route.
- **Environment**: Informational, **not** a recipe or a mob-drop category.

## Verification checklist

- [x] Compared changes introduced by GitHub head 0.31.55-b to the preceding 0.31.52-b snapshot.
- [x] Read authoritative `BasinMaterialProcessingSystem`, `BasinBlockEntity`, `OreMiningRules`, `BrickFurnaceRecipeRegistry`, `RecipeManagerMixin`, environment effects and release notes.
- [x] Updated category source definitions, recipe notes, and metadata.
- [x] Source-static regression assertions for v55 rules and new items.
- [ ] **Full Gradle Fabric compilation** (not available in this container; Gradle distribution not cached, outbound download blocked).
- [ ] **Singleplayer recipe-display browse and A/U search** for all 10 Basin materials, Axle and alternate Basin.
- [ ] **Singleplayer survival**: exact chance display, output slots, 60-tick Hardwood table, all stone/ore permits, non-mineable Deepslate.
- [ ] **Dedicated multiplayer**: sync type and fallback, all recipe placement, light/Y thresholds, Nest inventory recovery.
- [ ] **Forge/NeoForge compatibility** (not covered by this Fabric-targeted patch).

**Scope warning:** The checked boxes are evidence of source comparison and static checks; they are not claims that the new displays compile or operate correctly in the Minecraft client.
