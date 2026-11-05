# PF-EMI pf.18 / ProjectFlint 0.31.57-b — Source parity and presentation

**Pinned upstream:** `bostonstuddard/ProjectFlint` commit `7e7321a06daff837866c6ba95c4436f2e83c8824`; Minecraft 1.21.11 Fabric. This file is for developers and testers, not an in-game guide.

## Basin: exact source behavior

Reference: `BasinBlockEntity.requiredStompsFor`, `BasinMaterialProcessingSystem.process`, and `docs/BASIN_MATERIAL_YIELDS.md` at the pinned commit.

| Input (one) | Stomps | Guaranteed / 50:50 outputs | Independent bonus rolls |
|---|---:|---|---|
| Any planks | 2 | Wood Dust ×1 | — |
| Stripped Oak/Birch Logs/Wood | 3 | Tannin ×2 | — |
| Raw Iron | 3 | Iron Dust ×1 | Iron Dust +1 75%; Slag 50%; Gold Dust 3% |
| Raw Copper | 3 | Copper Dust ×1 | Copper Dust +1 83%; Copper Dust +1 24%; Nickel Dust 10%; Silver Dust 2%; Slag 5% |
| Andesite/Granite | 3 | Fertilizer ×1 or ×2 (50:50) | Stone Dust 33% |
| Stone/Cobblestone | 3 | None | Stone Dust +1 66%; +1 44%; +1 13.33% |
| Diorite | 3 | Stone Dust ×1 or ×2 (50:50) | — |
| Stone Dust | 2 | None | Slag 50% |
| Gravel | 2 | Flint ×1 | Quartz Dust 20%; Iron Dust 1% |
| Sand | 2 | Potassium Dust ×1 | Tin Dust 15%; Mica Dust 5%; Gold Dust 1% |
| Clay Block | 3 | None | Mica Dust 5%; Rare Earth Dust 3.33%; Silver Dust 0.33%; Gold Dust 2% |
| Rare Earth Dust | 3 | None | Lithium Dust 22.3%; Iron Dust 15%; Aluminum Dust 4.8%; Gallium Dust 0.85% |
| Slag | 3 | None | Iron Dust 33%; Copper Dust 33%; Silicon Dust 33% |

All bonuses roll **independently**, including repeat-output rolls. On exclusive 50:50 outcomes exactly one quantity is produced. A wash can yield zero drops if no roll succeeds. **One food point is charged on every valid stomp**, not on insertion/removal and not just once per finished item. Creative has no food cost. Valid stomps require available food; on completion, one input item is consumed. The physical Basin stores a real item stack and partial stomp progress and synchronizes them to clients (added in 0.31.56-b). PF-EMI does not implement these mechanics; it documents them.

## Recipe ownership

- **Basin**: the fifteen input-specific recipe pages (thirteen grouped rows in the source reference) above; shown with compact visual slots and chance tooltips, never as crafting recipes. Planks and stripped wood have their actual counts and stomp costs.
- **Personal Crafting 2×2**: bootstrap Hardwood Table recipe: two Hardwood Alloy + one Cobblestone + one Bound Wood Processor. Leather Sheet + Bone Knife → three Straps; Strap + two String → two Repair Kits. Native output/remaining-item behavior is unchanged.
- **Hardwood Crafting Table 3×3**: ProjectFlint `hardwood_crafting` recipe type (or bundled active JSON fallback when not synchronized) supplies the new tools, sheets/boards, glass/insulated glass dust sequence, and the other new 0.31.57 recipes. The table's 60-tick time is unchanged. Durable tools are catalysts/reusable with wear, including recipes using both Bone Knife and Flint File.
- **Fireclay Forge**: Hardwood + complete five-minute Charcoal burn → Hardwood Alloy; MBSSIG Dust + complete five-minute Charcoal burn → Pure Mica Insulated Glass. Logs → Charcoal are a separate fuel process. These are not Hardwood Table recipes.
- **Brick Furnace, block drops, crop growth and mob loot**: existing categories preserved. Ordinary world block/loot rules remain controlled by ProjectFlint.

## Player-facing changes

Basin entries show one short cost line, input + Basin catalyst, and up to six candidate results. Hover results for exact odds. This avoids long paragraphs, misleading universal guarantees, and cut-off output slots. Other world/loot informational pages display no more than two hint lines; longer details appear on source-item hover. Recipe UI is not developer documentation; never place notes about source files, registries, version numbers or patches in visible game content.

## Tests outstanding

- [x] Pinned GitHub source and exact Basin processing/yield definitions reviewed.
- [x] Source code contains each v0.31.57 Basin input, stomp count and chance.
- [x] Fireclay Forge MBSSIG recipe and personal Hardwood Table bootstrap included.
- [x] Player-facing Basin pages are visually compact at source level.
- [ ] Gradle 9.2.1 Fabric compile and game launch in supported development environment.
- [ ] In-game screenshot/GUI scaling inspection, hover tooltip testing and A/U recipe navigation.
- [ ] Singleplayer and dedicated-server Basin stomp-cost, progress/persistence, item rendering and RNG parity tests.
- [ ] Confirm custom recipe sync/fallback, 160+ Hardwood outputs, container returns and durability with actual mod JAR.
