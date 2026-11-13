# PF-EMI 26.1.2 — Issue #6, navigation fix (Test 4)

Baseline inspected: `26.1.2` branch, and the previous Test 3 recipe-grid fix must already be applied to the checkout.

## Why

PF-EMI `EmiRecipes.Manager` keys its `byOutput` and `byInput` maps with
`EmiStackList.ComparisonHashStrategy`; the strategy looks up its comparison by
item key using `EmiComparisonDefaults.get`.

The previous default comparison ignores components. All Iron Jetpacks tiers use
the same `ironjetpacks:jetpack` item with a tier-specific data component. This
causes clicking Copper to look up recipes for all tiers, opening Wooden first.

## Change

`EmiComparisonDefaults.java` assigns component-aware comparison to only four
Iron Jetpacks item keys: `jetpack`, `cell`, `thruster`, `capacitor`. Existing
explicit comparison overrides remain higher priority. Every other item keeps
the existing default comparison. This affects recipe navigation/indexing, not
crafting mechanics. This patch does not include the previous Test 3 files.

## Test

1. Overlay the ZIP into the root of the **26.1.2** repository; overwrite the
   one Java file.
2. Keep Test 2 and Test 3 changes in your working branch (the ZIP is incremental).
3. Build with the project's 26.1.2 build script and Java 25.
4. Open the recipe for Wooden, Copper, and Emerald Jetpacks individually.
   Clicking Copper must open Copper, not Wooden.
5. Check uses of Copper and tier-matched capacitor/thruster recipes.
6. Check unrelated mod items still show their normal uses and recipes.

## Validation limits

A standalone Java mock verifies this source compiles with the expected Mojang
registry/item types and that the four keys receive comparison-by-components,
while unrelated items remain item-only. No full mod Gradle build or in-game
runtime test has been executed here. This is a test patch, not a confirmed fix.
