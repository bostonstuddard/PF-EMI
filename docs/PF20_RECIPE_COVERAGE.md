# PF-EMI pf.20 — New and changed recipe coverage (v0.31.61 → v0.31.67)

**Source:** `bostonstuddard/ProjectFlint`, v0.31.67-b, pinned commit `b0d13129e38fdc3e38fd373bce97e9d41cbebc90`.

Items listed as **runtime indexed** are not hand-written approximations. PF-EMI reads their actual `projectflint:hardwood_crafting` recipe delegates, and can supplement unresolved wrapped custom recipes from the installed mod's exact resource JSON. This is source-code coverage, **not proof that all pages opened in game**.

## Restored / changed personal 2×2 recipes

- [x] `wooden_pickaxe` — Stick, String, Leather Strap, Wooden Sheet.
- [x] `wooden_axe` — Tannin, Leather Sheet, Stick, Wooden Sheet.
- [x] `wooden_shovel` — Oak Planks, Stick, Leather Strap (other source-defined wood variants also indexed).
- [x] `wooden_sword_personal` — 2 Wooden Sheets + Stick.
- [x] `wooden_hoe_personal` — 2 Wooden Sheets + Stick + Plant Tool Binding.
- [x] `leather_straps_from_sheet` — personal yield 2; Hardwood version 3.
- [x] `leather_repair_kit` — personal yield 1; Hardwood version 2.
- [x] Personal Sheets/Boards use the current defined output counts; Hardwood Sheets/Boards have larger output counts.
- [x] `primitive_wooden_sheet_from_stripped_log` — removed from PF-EMI display; deleted upstream by the v0.31.67 cleanup script.
- [x] Flint Axe does not count as a Log → Planks axe; only Wooden and Hardwood Axes are displayed.

## v0.31.65 Hardwood building additions — runtime indexed

- [x] `hardwood_clay_block`
- [x] `hardwood_dirt_from_loose_soil`
- [x] `hardwood_dirt_slabs`
- [x] `hardwood_dirt_stairs`
- [x] `hardwood_loose_dirt_slabs`
- [x] `hardwood_loose_mud_brick_slabs`
- [x] `hardwood_loose_mud_bricks`
- [x] `hardwood_loose_soil_from_dirt`
- [x] `hardwood_mud_brick_slabs`
- [x] `hardwood_mud_brick_stairs`
- [x] `hardwood_mud_brick_walls`
- [x] `hardwood_mud_bricks`
- [x] `hardwood_mud_from_dirt`
- [x] `hardwood_mud_from_dirt_vanilla_bucket`
- [x] `hardwood_mud_piles_from_mud`
- [x] `hardwood_packed_mud`

## v0.31.66 new Hardwood progression — runtime indexed

- [x] `hardwood_black_powder_gunpowder` — 4 Black Powder output.
- [x] `hardwood_black_powder_potassium` — 3 Black Powder output.
- [x] `hardwood_dynamite` — 1 Dynamite in exact source-defined 3×3 placement.
- [x] `hardwood_fuse` — Fine Fuse Powder + Rope.
- [x] `hardwood_fuse_ready_dust` — Silicon, Aluminum, Tin, Silver Dust.

## Processing and world interactions — manually source-mapped

- [x] Wooden Grinder: Loose Mud Bricks 4 Fired Clay + 8 Mud Piles, Bricks 2 Loose Mud Bricks + 2 Wooden Screws.
- [x] Basin: Coal (3 stomps), Charcoal (2 stomps), independent Sulfur/Carbon/Stone/Wood Dust odds.
- [x] Brick Furnace only: Fuse-Ready Dust → Fine Fuse Powder in 200s; no Brick Blast Furnace claim.
- [x] Fireclay Forge: Carbon Dust + 2 successive Charcoal charges → Pure Carbon Dust, 400s.
- [x] Dynamite: eight source-defined blastable stone/ore types, 4.5s fuse, 1.5x independent drop-roll probabilities.
- [x] Crude Stone Hammer processing: Stone → Crushed Stone plus four Rocks.
- [x] Packed Mud: 10% movement speed when walking on the block.

## Still requiring live verification

- [ ] Compare **every** Hardwood recipe in EMI against its true registered recipe and durable-tool remainder.
- [ ] Check all 3×3 shaped grids, alternative ingredient tags and item hover links.
- [ ] Test custom inventory recipe-fill buttons and recipe trees on singleplayer + dedicated server.
- [ ] Compare Basin/Forge/Furnace processing with gameplay ticks and inventory outputs.
- [ ] Test Dynamite radius, damage, blast resistance and exact random drops in game.
- [ ] Run full Gradle compilation and smoke test.
