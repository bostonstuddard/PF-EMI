# PF-EMI 26.1.2 — Iron Jetpacks Issue #6 — Test 2

**Target branch:** `26.1.2`  
**Tested source base:** `78c91d81527891fdc955f7e0ff51740c393ea8fa`  
**Patch type:** replacement source file(s), relative to the repository root.

## Change

- `xplat/src/main/java/dev/emi/emi/api/stack/ItemEmiStack.java`
  - Preserve the *effective* item data components when converting a `net.minecraft.world.item.ItemStack` into an `ItemEmiStack`.
  - Derive component additions, overrides, and removals relative to the registered item's default components, rather than copying `ItemStack.getComponentsPatch()` (which is relative to the original holder's prototype).
  - Addresses item alternatives supplied through `Holder.direct(item, components)`, as Iron Jetpacks' custom tier ingredient does.
  - No Iron Jetpacks dependency or special-case added; ordinary items should remain unchanged.

## Why the first patch wasn't enough

The prior 26.1.2 commit already preserves the direct holders when parsing recipe ingredients. However, `EmiStack.of(ItemStack)` calls the `ItemEmiStack` constructor, which formerly copied `stack.getComponentsPatch()`. On an `ItemStack` initialized using a synthetic holder with custom default components, this patch may be empty. Later `ItemEmiStack.getItemStack()` reconstructs the stack with the regular registered item holder and loses those custom components. This test fixes that second conversion.

## Install and build

1. Extract this archive **into the root of the updated 26.1.2 PF-EMI checkout** (allow the Java file to overwrite).
2. Use **Java 25** and run `build-26x.bat` (or the existing build workflow).
3. Test the generated NeoForge mod JAR on **Minecraft 26.1.2 / NeoForge**, with **Iron Jetpacks 26.1.2-9.0.3**, ideally against the same ATM 11 installation.
4. **Remove the old PF-EMI JAR** before launching, so only one version is present.
5. Compare the same jetpack upgrade recipe in EMI and JEI, including slot cycling and hovered ingredient names; verify the shown previous-tier jetpacks and their components. Check other component-sensitive recipes too.
6. If still incorrect, collect the screenshots and `latest.log` from a clean reproduction.

## Validation / limitations

- The effective-component patch routine was extracted verbatim from the changed source and compiled with Java using minimal test doubles; 7 component round-trip scenarios passed (multiple jetpack tiers, defaults, add, override, remove, and mixed cases).
- Minecraft 26.1.2 decompiled public source confirmed constructor and component API signatures used here.
- **A full Gradle build and in-game test have not been run** in this environment (Java 25 and cached project dependencies not available).
- The original reporter's linked GitHub log ZIP could not be downloaded by the current GitHub connector. The proposed fix is supported by source inspection, not by independently parsed log contents.

Issue: https://github.com/bostonstuddard/PF-EMI/issues/6
