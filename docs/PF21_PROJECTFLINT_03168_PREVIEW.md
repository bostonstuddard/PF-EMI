# PF-EMI pf.21 — ProjectFlint v0.31.68-b compatibility preview

**Reference baseline:** ProjectFlint main commit `b0d13129e38fdc3e38fd373bce97e9d41cbebc90` (v0.31.67-b). The v0.31.68-b changes below were supplied as the upcoming design, **not** verified from released code. This is a PF-EMI source patch for Fabric / Minecraft 1.21.11; it changes recipe-viewer displays only.

## Confirmed from the user's upcoming specifications

### Weak Dynamite: crafting pattern

Hardwood Crafting Table only; exact shaped 3×3 target:

```text
P F P
P R P
C C C

P = Black Powder (4 total)
F = Fuse (1)
R = Red Dye (1)
C = Copper Dust (3 total)
```

The existing pf.20 integration reads real shaped recipes from the synchronized `projectflint:hardwood_crafting` recipe type. When synchronization is incomplete, it parses the actual JSON bundled in the installed ProjectFlint mod. It does **not** hardcode an unreleased alternative while v0.31.67-b is still installed. As soon as ProjectFlint ships the target shape, both pathways display those exact 9 slots. This must be checked in-game after that push.

### Weak Dynamite: output probabilities

Only **after** detecting ProjectFlint version 0.31.68 or newer, individual *per-block* material yield probabilities use a `1.20×` multiplier instead of `1.50×`. Each roll is independent.

- 50% base → **60%** boosted.
- 75% base → **90%** boosted.
- 100% base → **1 guaranteed item, 20% chance of one additional item**.
- 83% base → **99.6%** boosted.
- 2.4% base → **2.88%** boosted.

These chances apply *only if the corresponding block was destroyed*. They do not indicate how many blocks Dynamite will destroy. The explosion's radius, ore selection, limited block count and the higher resistance of ordinary rock are enforced in ProjectFlint, **not** PF-EMI. The specification does not state an exact new radius, maximum rock count or block-destruction probability; none has been invented in the UI.

### Restricted mining

The PF-EMI Block Drops/Recovery category gets an **informational, no-output** Dripstone entry for Dripstone Block / Pointed Dripstone, visible only with ProjectFlint >=0.31.68. No fake drop is registered. Other as-yet-unnamed restrictions are not guessed; their authoritative tool permissions must be audited against the released v0.31.68 `OreMiningRules` and related server hooks.

## Compatibility and installation

- With **ProjectFlint v0.31.67-b or earlier**, pf.21 keeps the existing 1.50× blasting display and reads the actual older Dynamite pattern. It does not show the proposed Dripstone restriction prematurely.
- With **ProjectFlint v0.31.68-b or later**, pf.21 shows the 1.20× blast yields, the reduced-blast note and the Dripstone restriction. The Dynamite grid still comes from the game's real recipe source; no PF-EMI recipe override is fabricated.
- The change from `1.50×` to `1.20×` has **not** been implemented in ProjectFlint by this ZIP. If the upcoming ProjectFlint release differs, PF-EMI must be compared against its actual final source and adjusted.

## Verification checklist

- [x] Verified upstream GitHub `main` was v0.31.67-b at the time of preparation.
- [x] Inspected pf.20 Dynamite recipe registration, boosted-yield calculator and hardwood JSON importer.
- [x] Version-gated future displays to prevent v0.31.67 misinformation.
- [x] Preserved individual chances and guaranteed overrolls; arithmetic assertions are in `tools/verify_pf_emi_pf21.py`.
- [x] Kept conditional Dynamite result displays under Dynamite Blasting, not ordinary crafting or Basin processing.
- [x] Verified source-relative flat patch ZIP layout.
- [ ] Verify v0.31.68-b has actually been published with the requested changes.
- [ ] Compare its final Dynamite JSON, radius, stone-vs-ore destruction chance and complete locked-block list.
- [ ] Run Gradle compilation on Java 21 / Minecraft 1.21.11 Fabric.
- [ ] Test the exact new 3×3 recipe and EMI fill/recipe tree in-game.
- [ ] Test 1.20× probability display and guaranteed overroll tooltips in-game.
- [ ] Test Dripstone/other restricted-block drops with all available tools in Survival.

No runtime certification is claimed from these source checks.
