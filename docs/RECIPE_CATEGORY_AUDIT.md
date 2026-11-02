# PF-EMI pf.14 — Category placement and UI audit

**Upstream reference:** bostonstuddard/ProjectFlint `0.31.50-b`, pinned commit `169797e5440474457fd3410456bf43401214bf3c`, Minecraft `1.21.11`.

This document answers the pf.12 problem: newly added recipes were indiscriminately assigned to the fabricated **Recent World Processing** category. This category has been removed. Recipe categories now match the actual gameplay workstation or action. These are *EMI display registrations*, not ProjectFlint gameplay registrations.

| Category in EMI | Real action / station | Changes in pf.13 | Source checked |
|---|---|---|---|
| **Personal Crafting (2x2)** | Player's Vest-enabled personal inventory grid | Dedicated 2×2 recipe pages for generated personal recipes, all nine normal wood variants, Hardwood material, Hardwood Crafting Table construction, Fat-Burning Torch crafting, leatherworking and other portable conversions | `PersonalCraftingGuideRegistry.java` and sampled `data/projectflint/recipe/*.json` |
| **Hardwood Crafting Table (3x3)** | Hardwood Crafting Table | Exact-shaped Hardwood Axe, Pickaxe, Shovel, Hoe, Sword, Hammer; tooltip notes 3-second craft delay | Exact six Hardwood tool recipe JSONs; `HardwoodCraftingMenu.java` |
| **Basin** | Jump on a Basin holding 1 wood to process it | Tannin from stripped Oak/Birch, Wood Dust from Planks moved out of catch-all | `BasinBlockEntity.java` |
| **Fireclay Forge** | Formed multiblock controller + fuel and firestarter | 4 burnable logs/wood → 4 Charcoal, and charcoal fire + Hardwood → Hardwood Alloy moved to their own category | `FireclayForgeBlockEntity.java`; `FireclayForgeMultiblock.java` |
| **Farming / Crop Harvests** | Strawberry plants in the world | Strawberry seed placement and mature-plant harvesting moved to farming; **no fake workstation** | `StrawberrySeedsItem.java`; `PrimitiveInteractionSystem.java` |
| **World Placement and Interactions** | Placing, refueling or manually modifying world blocks | Fat-Burning Torch refueling now lives beside Sleeping Bag placement and other world operations | `FatBurningTorchBlock` behavior and existing PF-EMI routes |
| **Wooden Grinder** | Two-input Wooden Grinder and its compatible single-input Clay Grinder recipes | All Clay Grinder conversions mirrored under Wooden Grinder with distinct IDs; returned Clay Bucket byproducts; Pumpkin Cake slicing; ProjectFlint Brush upgrade | `WoodenGrinderRecipeRegistry.java`; `WoodenGrinderMenu.java`; `ProjectFlintBrushSystem.java`; `ProjectFlintRecipeGuideScreen.java` |
| **Clay Grinder** | One-input Clay Grinder | Existing source-backed one-input conversions retained | `ClayGrinderRecipeGuideRegistry.java` |
| **Sanding Station** | Station durability-powered sanding | Adds 25 flower/ink dye operations, exact source-defined counts | `SandingStationRecipeRegistry.java` |
| **Primitive Oven** | Direct cooking + researched parchment cooking | Adds direct food cooking/brick-bucket firing separately from bound-parchment research pages | `PrimitiveStoveRecipeRegistry.java`; `PrimitiveProcessingRegistry.java` |
| **Brick Furnace** | Ordinary Brick Furnace | Leather → Tanned Leather and Sand → Glass (the Brick Blast Furnace must NOT receive these two) | `BrickFurnaceRecipeRegistry.java`; `BrickFurnaceBlockEntity.java` |
| **Bookshelf Research** | Four installed books and blank/staged Recipe Parchment | Existing pf.12 bound parchment metadata preserved | `BookshelfRecipeRegistry.java`; `RecipeParchmentItem.java` |
| **Block Drops / Recovery** | Tool-dependent breaks | Existing block loot reference retained | Source audit incomplete |
| **Mob Drops** | Entity loot | Existing mob reference retained | Source audit incomplete |
| **Structure Guides** | Isometric multiblock construction | Previous visual definitions unchanged by this patch | Needs in-game inspection |

## Recipe integrity / duplication safeguards

- Native `projectflint:*` crafting recipes that were independently ported are removed from EMI's **generic vanilla Crafting category** only when their exact recipe ID appears in the pf.13 mapping. They are redisplayed at the correct ProjectFlint crafting workstation. *ProjectFlint's server-side recipe registrations are untouched.*
- Existing native crafting recipes not individually mapped remain visible under their existing categories, rather than being silently deleted. Dynamic Leather Repair is deliberately left native because it preserves damage/components and a static output would lie about its result.
- Each mirrored Clay Grinder conversion receives a separate ID under `wood_clay/*` when it is also shown at Wooden Grinder; duplicate recipe IDs between stations are avoided.
- Grid pages preserve the **shape** of six Hardwood tool patterns rather than collapsing them into unordered ingredient lists.
- Fabric is the only loader enabled by the supplied source; Forge and NeoForge testing has not happened here.

## Explicit limitations — not a claim of complete parity

The exact source tree is not available locally as one checkout; individual registries and JSONs were inspected through the linked GitHub repository. Full recipe enumeration, condition-dependent material use, all entity/block loot probabilities, recipe-book and 3×3 vanilla-crafting behavior, and inventory-locked recipes require additional verification in a running Minecraft instance. The source patch could not be compiled in this environment because Gradle 9.2.1 was not cached and network DNS was unavailable. See `VERIFICATION_REPORT.md` and the complete audit checklist.

## pf.14 source corrections and UI regression cases

- Latest source `0.31.49-b` removes Fat from Fat-Burning Torch crafting. Refueling still uses Fat on an already-placed torch.
- Hardwood Crafting Table assembly consumes **Bound Wood Processor**, not the assembled Lumber Processor.
- Every Hardwood 3×3 craft requires **60 server ticks** (3 seconds), regardless of recipe; the timer resets for each new output and on changed input.
- All 18 log/stripped-log conversion recipes accept either Wooden Axe or Hardwood Axe; the tool is retained and receives durability damage.
- ProjectFlint-native crafting JSONs not previously hardcoded are categorized by the actual loaded recipe geometry (2×2 personal or 3×3 Hardwood) and deduplicated from vanilla Crafting.
- Tanned Utility Vest remains **Wooden Grinder ONLY**; generic crafting entries displaying it are excluded.
- Cow milking needs a Fired Clay Bucket and an *adult living cow*; output is a Clay Milk Bucket and does not involve killing the mob.
- Structure rails use actual `SlotWidget` instances; verify left-click, A (recipe), U (uses), and bookmark interaction on input and output slots, including Wooden Beam alternatives.
- Verify dark default text on the Light Gray recipe surface, note wrapping with narrow resolutions, shapeless icon removal and visible time caption.

**Open runtime checks:** Gradle build; EMI Light/Dark theme; screen scales 1–4; 3×3 timing in a running dedicated server; Forge/NeoForge behavior (not included in this Fabric-only patch); all source recipes/loot runtime outcomes. Static verification does NOT prove these checks.
