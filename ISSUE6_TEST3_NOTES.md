# PF-EMI 26.1.2 — Issue #6, test patch 3

## Source baseline

- GitHub branch: `bostonstuddard/PF-EMI` / `26.1.2`
- Baseline HEAD inspected: `e058c2ef8fd9c74606759d7d85f44827917106fe`
- This patch expects the previous Issue #6 ingredient and ItemEmiStack component-preservation fixes to already be present on that branch.

## Changed file

`xplat/src/main/java/dev/emi/emi/recipe/EmiShapedRecipe.java`

## Fix

For shaped recipes not represented by Minecraft's ShapedRecipe class (notably Iron Jetpacks' JetpackUpgradeRecipe), PF-EMI read `placementInfo().ingredients()`. That list removes empty crafting slots. For an Iron Jetpacks upgrade recipe with pattern `MCM / MJM / T T`, the right-hand thruster was wrongly displayed in the bottom-middle slot.

The patch reconstructs the actual 3x3 grid from `ShapedCraftingRecipeDisplay` before creating the EMI recipe. When possible, it resolves per-slot `SlotDisplay` values into full ItemStacks, preserving NeoForge DataComponentIngredient-specific capacitor/thruster variants (and the previous-tier jetpack) instead of using only bare item-holder IDs. It retains fallback values for tags and unsupported displays. Shapeless recipes are not rewritten.

## Applying and testing

1. Unzip this archive into the ROOT of the 26.1.2 PF-EMI source checkout. Keep subfolder paths and overwrite the single Java source file.
2. Build using the existing `build-26x.bat` with Java 25.
3. Deploy `release/PF-EMI-26.1.2-NEOFORGE.jar` to the reporter's NeoForge 26.1.2.126 / Iron Jetpacks 9.0.3 test setup.
4. Inspect the same jetpack upgrade recipe in EMI. Expected: `T [empty] T` bottom row, correct capacitor and thruster tiers, and the previous-tier jetpack in the middle.
5. Ask for a screenshot and report whether the crafting components and slot layout now match JEI.

## Validation status

- The replacement Java file was compiled with Java 21 against lightweight API stubs (syntax and mocked call compatibility only).
- A mock regression scenario passed for the 3x3 `MCM/MJM/T T` layout, preserved bottom-center blank, and component-bearing capacitor/thruster/jetpack displays.
- Full Minecraft Gradle compile and runtime test were **not** executed: the target needs Java 25 and external mod dependencies.

**Do not mark issue #6 fixed until the reporter confirms in-game.**
