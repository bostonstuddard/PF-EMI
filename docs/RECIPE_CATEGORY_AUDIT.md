# PF-EMI Recipe Category Audit — ProjectFlint 0.31.57-b

**Category ownership (current):**

| Category | What belongs here |
|---|---|
| Personal Crafting (2×2) | Portable small-grid recipes, including the first Hardwood Crafting Table, Leather Straps and Repair Kits |
| Hardwood Crafting Table (3×3) | Every recipe whose source type is `projectflint:hardwood_crafting` (shaped, shapeless, durable), including the newly added dust/glass progression |
| Basin | Physical one-item stomp processing; 2–3 stomps, one food point per stomp, independent chance outputs |
| Fireclay Forge | Hardwood Alloy and Pure Mica Insulated Glass heated over one complete Charcoal firing, and wood-fuel Charcoal byproduct |
| Wooden/Clay Grinder, Sanding Station, Kilns, Brick Furnace | Machine-specific recipes only; no inferred ordinary crafting duplicates |
| Block Drops, Mob Loot, World Interactions, Farming, Underground Hazards | Contextual displays, not crafting stations |

The generic newly-added-content category is not used. Do not route by output name when a distinct source recipe type or workstation exists. Source audit: `docs/PF18_PROJECTFLINT_0.31.57_PARITY.md`. Actual runtime recipe availability may depend on the connected server; test separately.
