# PF-EMI pf.23 — Verification Report

## Source versions

- **PF-EMI baseline:** v1.1.23-pf.22, reconstructed from the pf.11 full-source archive and each sequential pf.12–pf.22 patch.
- **New PF-EMI version:** v1.1.23-pf.23.
- **ProjectFlint source:** v0.32.1 on GitHub `main`, commit `796d129dadbab83dc46dab1b5652b2bafe92eb3d`.
- **Target:** Minecraft 1.21.11 Fabric, Java 21.

## Completed checks

- `python tools/verify_pf_emi_pf23.py`: **42/42 focused source checks passed**.
- Parsed `ProjectFlintPlugin.java` with Java 21 `JavacTask.parse()`: **no syntax diagnostics**. This does NOT check missing symbols, imports, mappings, or linked runtime APIs.
- Root-relative patch ZIP: no enclosing folder, no duplicate paths, CRC and expected change-set verification performed during packaging.
- Recipe changes compared with the actual v0.32.1 registry, recipe JSON, and release notes; items not registered as recipes are kept as world/loot information.

## Unverified and remaining work

- Full Gradle build **NOT completed**. The Java wrapper could not resolve `services.gradle.org` when requesting Gradle 9.2.1 (`UnknownHostException`).
- Minecraft launch, recipe placement, EMI crafting-tree navigation, `+`/transfer buttons, singleplayer and multiplayer **NOT tested**.
- Test the new Water Pot interaction with both empty and partially filled pots, return of the Clay Bucket, and the 3×3 Hardwood Knife recipe's durability/remainder behavior.
- Check the upgraded Project Flint Brush's 35-tick use speed and whether 4–5 sheep Wool is displayed with the correct model.
- Verify Hardwood crafting IDs from the installed 0.32.1 mod: output 3 Hardwood, 4 Cabinets and 1 Unfired Clay Bucket with Knife wear. Do not rely on a bundled or stale 0.31.x mod.
- Check raw leather guarantee for adult cows and 2–3 Raw Leather for adult Black/Brown Bears in the actual loot tables.
- Check the Wooden Grinder's new recipes, two-input recipe transfer, the removed conversions, and the Advanced Kiln's normal/special Flint costs.

## Exclusions

Cooking balance issue #25 and animal-byproduct uses issue #17 remain **proposals only**, not v0.32.1 gameplay additions. No associated EMI craft was created.
