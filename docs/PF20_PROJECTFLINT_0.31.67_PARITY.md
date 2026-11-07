# PF-EMI pf.20 — ProjectFlint v0.31.67-b source-parity audit

**Pinned main commit:** `b0d13129e38fdc3e38fd373bce97e9d41cbebc90`. Minecraft **1.21.11**, Fabric, Java 21. Source was compared with GitHub's upstream releases 0.31.62 through 0.31.67 and current gameplay registries. The player-facing screens should contain only short gameplay instructions.

## Categorized changes

| Source release | Current behavior | PF-EMI representation |
|---|---|---|
| .62 | Crude Stone Hammer converts Stone to Crushed Stone and four Rocks; Multitools also work | Stone processing under Block Drops / recovery (not regular ore mining) |
| .63 | Successful manually transformed blocks count as player breaks for Flint Quests | No recipe or fake drop; event occurs in ProjectFlint server |
| .64 | Personal wooden tools, Sword/Hoe; Oak Sheets/Boards; Flint Axe temporarily permitted for planks | Explicit personal tools and sheet/board conversions; later .66 Flint Axe reversal takes precedence |
| .65 | Mud/stone building recipes, Dirt Slabs/Stairs, Packed Mud speed, reduced mud material costs | Hardwood recipe-type lookup and bundled-JSON recovery; Wooden Grinder material corrections; Packed Mud world info |
| .66 | Coal/Charcoal Basin, Fuse, two Black Powder crafts, Dynamite, 200s fuse furnace, 400s Forge carbon | Basin, Hardwood Table, Dynamite Blasting, Brick Furnace and Forge respectively |
| .67 | Temporary Stripped Log to Sheet personal craft removed | Excluded from EMI even if obsolete JSON remains in a patched-up source folder |

## Important exact mechanics

- **Personal 2×2:** Wooden Pickaxe (Stick, String, Leather Strap, Wooden Sheet); Axe (Tannin, Leather Sheet, Stick, Wooden Sheet); Shovel (Oak Planks, Stick, Leather Strap); Sword (2 Wooden Sheets, Stick); Hoe (2 Wooden Sheets, Stick, Plant Tool Binding). Personal Leather Straps yield 2, Repair Kit yields 1.
- **Hardwood 3×3:** source-defined recipes remain authoritative. New Dirt Slabs/Stairs, mud conversion/stairs/walls, Fuse, Black Powder, Dynamite and Glass recipes are discovered through `projectflint:hardwood_crafting` or the installed mod's corresponding JSON. Missing durable wrappers no longer silently disappear when some synchronized Hardwood recipes exist.
- **Wooden Grinder:** 4 Fired Clay + 8 Mud Piles → 1 Loose Mud Bricks; 2 Loose Mud Bricks + 2 Wooden Screws → Bricks. A Wooden Beam + Flint File → 4 Wooden Sheets (File wears).
- **Basin:** one food point per stomp; Coal uses 3 stomps (45% Sulfur Dust, 43% and 23% independent Stone Dust rolls, 2.4% Carbon Dust); Charcoal uses 2 (20% Sulfur Dust, 15% Wood Dust). Andesite/Granite each guarantee one Fertilizer with 50% for another; Diorite guarantees one Stone Dust with 50% for another.
- **Brick Furnace only:** 1 Fuse-Ready Dust → 1 Fine Fuse Powder, **4,000 ticks = 200s**. Not a vanilla furnace or Brick Blast Furnace process.
- **Fireclay Forge:** Carbon Dust → Pure Carbon Dust requires **8,000 ticks = 400s** and two valid Charcoal charges. Hardwood Alloy and Pure Mica Insulated Glass still need 6,000 ticks each.
- **Dynamite:** main-hand Dynamite with offhand Firestarter/Flint and Steel, fuse 90 ticks (4.5s), radius 2.75 blocks. Only Stone, Cobblestone, Granite, Andesite, Diorite, Coal Ore, Iron Ore, Copper Ore are affected. Each Basin material roll is multiplied by 1.5 (including odds above 100% → guaranteed plus fractional chance). This is a **world interaction**, not an ordinary crafting or Basin stomp.
- **Flint Axe:** no longer supported for personal Log → Planks. No Stripped Log + Bone Knife early Sheet route in .67.

## Source mapping / implementation notes

| PF-EMI code | Upstream source of truth |
|---|---|
| 2×2 grid | `data/projectflint/recipe/wooden_*` and `PersonalCraftingGuideRegistry.java` |
| 3×3 grid | `ModRecipes.HARDWOOD_CRAFTING`, `HardwoodCraftingRecipe.java`, `data/projectflint/recipe/hardwood_*.json` |
| Grinder | `WoodenGrinderRecipeRegistry.java` |
| Basin | `BasinMaterialProcessingSystem.java`, `BasinBlockEntity.java` |
| Furnace | `BrickFurnaceRecipeRegistry.java`, `BrickFurnaceBlockEntity.java` |
| Forge | `FireclayForgeBlockEntity.java` |
| Dynamite | `DynamiteItem.java`, `DynamiteChargeBlockEntity.java` |
| Stone hammer | `OreMiningRules.java`, `ServerPlayerGameModeMixin.java` |

## Validation and outstanding checks

- [x] Pinned the .67 GitHub commit and reviewed .62–.67 release notes.
- [x] Corrected changed recipe costs, item quantities, and workstation ownership in source.
- [x] Confirmed all eight Dynamite targets and boosted roll logic against gameplay code.
- [x] Verified exact 200s furnace and 400s forge source durations.
- [x] Added both new Basin inputs and corrected source-defined rolls.
- [x] Included new personal tool routes and suppressed the retired recipe.
- [x] Verified patch ZIP root-relative file layout.
- [ ] Full Java/Gradle compilation against the requested client dependencies.
- [ ] Live in-game: every custom Hardwood wrapper and bundled recipe, including durable remainders.
- [ ] Live in-game: Wooden Grinder material costs and transfer; individual Basin output tooltips.
- [ ] Live in-game: Dynamite targets and explosion material yields; protected blocks unchanged.
- [ ] Live in-game: multiplayer/recipe-sync behavior and 2×2/3×3 craft transfer buttons.

These are source-level checks, **not** a claim of exhaustive in-game parity or a release-certified build.
