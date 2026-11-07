# PF-EMI pf.22 — ProjectFlint v0.31.69 compatibility

**Published baseline:** ProjectFlint v0.31.68-b (`acd9a193508efa3ebe2bcfdfc86a5e88f8e2e2b0`). **Future specification:** User-provided v0.31.69-b; not yet available on GitHub at time of preparation.

## Hardwood Crafting Table — additional Hardwood recipe

```text
DDD
DWD
TTT
```

- D = Wood Dust (five total)
- W = filled vanilla Water Bucket (one in center, empty Bucket returned)
- T = Tannin (three across the bottom)
- Output = 1 Hardwood

This **Hardwood Table-only** recipe is separate from the current personal 2×2 Hardwood craft. PF-EMI reads the actual synchronized ProjectFlint Hardwood recipe (or the bundled ProjectFlint recipe JSON fallback). It does not create an uncraftable fake entry. Once ProjectFlint 0.31.69 supplies the recipe, the three-by-three grid and returned Bucket appear in the correct EMI category. The user has explicitly requested a PF-EMI-only patch, so no ProjectFlint recipe file is included.

## Weak Dynamite — display rules

| Property | Published v0.31.68 | Planned v0.31.69 |
|---|---:|---:|
| Ore blast radius | 1.75 blocks | 3 blocks |
| Stone blast radius | 1.25 blocks | 2.25 blocks |
| Maximum ore blocks | 2 | 4 |
| Maximum stone blocks | 1 | 2 |
| Stone destruction chance | 50% | 65% |
| Basin material yield bonus | 20% (1.20×) | 25% (1.25×) |
| Fuse | 4.5 s (90 ticks) | 3 s (60 ticks) |

This PF-EMI-only patch does **not** modify ProjectFlint gameplay. Changes to explosion force, drops, and fuse must be made in ProjectFlint itself. EMI's version-gated display uses these numbers only if the installed ProjectFlint version is at least 0.31.69; 0.31.68 remains accurate in older instances. Independent rolls above 100% retain guaranteed drops plus one fractional chance. At 1.25×, a 50% Basin roll becomes 62.5%, and 100% becomes one guaranteed item plus a 25% chance for another.

## Mining tier — display rules

For installed ProjectFlint **v0.31.69+ only**, add Packed Mud and Mud Bricks under Block Drops / Recovery with Wooden/Hardwood Pickaxes and an intact recovery output. The requested mining behavior itself belongs to ProjectFlint and has not been added to this PF-EMI patch. Earlier versions retain their actual drop displays.

## Woven Chest → Wooden Chest — existing gameplay, missing EMI

The published ProjectFlint `WovenChestBlock.useItemOn` consumes **one Wooden Chest Frame** when used on an **existing placed Woven Chest**, replaces it with a Wooden Chest, and preserves stored contents. Added a world-interaction entry using the Frame as the consumed ingredient and the placed chest as the non-consumed target. This is not a crafting-grid recipe.

## Testing still required

- [x] Source-reviewed published v0.31.68 Hardwood type, Dynamite, and Woven Chest interaction.
- [x] v0.31.69 display constants and physical recipe pattern checked against user specification.
- [x] Source-relative patch-only ZIP and offline source checks.
- [ ] Test against the actual v0.31.69 release and compare its recipes, IDs, and runtime behavior.
- [ ] Compile xplat/Fabric with Gradle 9.2.1 and Java 21 in a complete build environment.
- [ ] Test 3×3 Hardwood grid and returned Water Bucket in EMI, on client and multiplayer server.
- [ ] Test A/U world-interaction lookups for Wooden Chest Frame, Woven Chest, and Wooden Chest.
- [ ] Test Dynamite probability tooltips at 1.25× and the Block Drops mining pages.
