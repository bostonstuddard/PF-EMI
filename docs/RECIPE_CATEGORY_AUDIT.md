# PF-EMI pf.15 — ProjectFlint 0.31.52-b category audit

GitHub source commit: `37c5f2af2a9939ac71c9d04664954caf10beeb7c`.

The major 0.31.51-b addition is a dedicated `projectflint:hardwood_crafting` recipe type. The previous fixed list of six Hardwood tools is no longer authoritative. The pf.15 integration queries that recipe type and unwraps `HardwoodCraftingRecipe` (and the durable delegate) into standard shaped/shapeless recipes. This preserves recipes for tools, blocks, beams, slabs, doors, paper/books, armor, dyes, Rope stages, and the new Hardwood Sticks.

## Classification

- **Hardwood Crafting Table (3x3):** All `projectflint:hardwood_crafting` entries, regardless of whether their recipe input could geometrically fit in 2x2. These are station-only by gameplay rule, not ordinary vanilla crafting. Always show the 60-tick per-craft delay.
- **Personal Crafting (2x2):** Genuine portable JSONs. Do not re-add retired `wooden_axe`, `wooden_pickaxe`, `wooden_shovel*`, or `*_door_hinged` variants. `hardwood_crafting_table` now has the station-only recipe type and cannot be represented as 2x2.
- **Sanding Station / Wooden Grinder / Clay Grinder:** Existing machine-specific operations stay in their own categories. New Hardwood alternatives at a table are *distinct* station recipes and must not replace the machine actions.
- **Block Drops:** v0.31.51-b OreMiningRules distinguish Stone/Coal, Iron/Copper stone variants, higher-tier ores and deepslate. Entries show the correct tool and yield; no unrestricted hand/food mining drop should be shown.

## Explicitly pinned upstream changes

- New registered item `projectflint:hardwood_sticks`; two Hardwood yield two Hardwood Sticks. Two planks yield three regular Sticks, but only at the Hardwood Table.
- Five normal Hardwood tools use 320 durability and Hardwood Hammer uses 560; their new shaped patterns include Leather Straps (Shovel excepted).
- Hardwood Table may use new durable recipes; `Bone Knife` / `Flint File` lose durability and the Unfired Brick Mold is reusable. Clay Water Bucket recipes return the empty bucket.
- New Hardwood station output paths include wooden slabs, beams, books, armor, forged tools, dyes with tripled outputs, masonry, fireclay, wooden storage, Well Pulley and staged Rope.
- Legacy early-stage smelting of Unfired Brick, Unfired Clay Bucket, and Unfired Fireclay Brick has been retired from the vanilla station category by ProjectFlint's `RecipeManagerMixin`; primitive heat-processing entries are separate.

## Known limitations / testing

- The source scan fallback mirrors built-in mod recipe JSONs when the synchronized custom type has **zero** entries. Server datapack removals and overrides cannot be inferred from bundled JSONs; test with actual multiplayer server packs.
- Build and in-game GUI/recipe tests have not run here; partial source checks alone are not a compatibility certificate.
- **Upstream bootstrap concern:** ProjectFlint's `hardwood_crafting_table.json` itself now uses the station-only `hardwood_shapeless` type. It appears to require the Hardwood Table to craft a Hardwood Table. PF-EMI mirrors the source rather than silently moving it to Personal Crafting; ProjectFlint may need a separate bootstrap route.
