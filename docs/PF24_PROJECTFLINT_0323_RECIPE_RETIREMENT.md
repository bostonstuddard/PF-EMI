# ProjectFlint v0.32.3 — PF-EMI Recipe Parity

**Pinned ProjectFlint source:** GitHub commit [`fe574310f03f33701dd481622f1655a4c70126bb`](https://github.com/bostonstuddard/ProjectFlint/commit/fe574310f03f33701dd481622f1655a4c70126bb). The gameplay change landed as 0.32.2; 0.32.3 fixes a Java initializer syntax error and makes no further recipe changes.

## Retired recipes (15)

The server's `RetiredRecipePolicy` rejects these IDs even when an old JSON/data pack survives:

| Retired ID (`projectflint:`) | PF-EMI action |
| --- | --- |
| `acacia_stripped_log_from_slabs` | Keep only Stripped Log → 2 slabs |
| `bamboo_stripped_log_from_slabs` | Keep only Stripped Bamboo Block → 2 slabs |
| `birch_stripped_log_from_slabs` | Keep only forward conversion |
| `cherry_stripped_log_from_slabs` | Keep only forward conversion |
| `dark_oak_stripped_log_from_slabs` | Keep only forward conversion |
| `jungle_stripped_log_from_slabs` | Keep only forward conversion |
| `mangrove_stripped_log_from_slabs` | Keep only forward conversion |
| `oak_stripped_log_from_slabs` | Keep only forward conversion |
| `pale_oak_stripped_log_from_slabs` | Keep only forward conversion |
| `spruce_stripped_log_from_slabs` | Keep only forward conversion |
| `loose_dirt_from_slabs` | Remove Clay + Wooden Grinder mirrored route `loose_dirt_from_slabs` |
| `loose_mud_bricks_from_slabs` | Remove Clay + Wooden Grinder mirrored route `loose_mud_block` |
| `sanded_wood_slabs_from_grinded` | Remove Clay + Wooden Grinder mirrored route `sanded_reverse` |
| `primitive_wooden_sheet_from_stripped_log` | Keep prior pf.20 retirement |
| `personal_pmi_sheets` | Keep prior pf.19 retirement |

## Preserved processes

- **Clay and Wooden Grinders:** Stripped Logs → wood slabs, 2 Sanded Wood Slabs → 1 Grinded Wood Slab, Fireclay/Mortar/Crude Clay balanced two-slab conversions, Loose Mud Bricks → 2 Loose Mud Brick Slabs, Loose Dirt → slabs only where actual station recipe exists, existing Woven Wood conversions.
- **Wooden Grinder:** 3 Sanded Wood Slabs + reusable Flint File → 2 Grinded Wood Slabs remains; no Grinded → 2 Sanded reverse. Materials are not generated from a loop.
- **Hardwood Crafting Table:** 2 full blocks → 6 slabs for Loose Dirt and Loose Mud Bricks; restored 0.32.1 outputs, ingredients and tool remainders remain.
- **Other PF categories:** no changes to the 0.32.1 Basin, Furnace, Stove, Bookshelf, Dynamite, mob drops, multiblocks, plus-button recipe transfer or crafting tree behavior.

## Source limitations

PF-EMI is a recipe viewer, not the recipe manager. The ProjectFlint gameplay mod's 0.32.2+ runtime recipe filter must enforce these retirements. This patch removes viewer-only shadow entries and rejects stale native recipe IDs to match it.

## Acceptance tests (in-game)

1. On ProjectFlint 0.32.3, check **both Clay Grinder and Wooden Grinder** recipes for Loose Dirt Slabs, Loose Mud Brick Slabs and Grinded Wood Slabs: no reverse entry to full material.
2. Confirm nine wood log types plus stripped Bamboo still process **forward** into wood slabs, and balanced Fireclay/Mortar slab reversals remain.
3. Confirm Hardwood Table crafting trees have no slab conversion profit loop, and no obsolete personal PMI Sheet/Stripped Log sheet routes.
4. Confirm world recipes, dedicated 3×3 Hardwood recipes, transfer (+), regular custom Grinder recipes, and multiplayer recipe refresh still operate.
