# PF-EMI 1.21.2 - Runtime Mixin Pass 8

This pass fixes the first runtime-only failures after Fabric and NeoForge both compiled successfully.

## Fixed

- `MouseMixin`
  - Minecraft 1.21.2 removed the `Screen.wrapScreenError(...)` calls from mouse input handling.
  - PF-EMI now injects immediately before the direct 1.21.2 calls to:
    - `Screen.mouseClicked(double, double, int)`
    - `Screen.mouseReleased(double, double, int)`
    - `Screen.mouseDragged(double, double, int, double, double)`
    - `Screen.mouseScrolled(double, double, double, double)`
  - This fixes the NeoForge startup crash reporting `MouseMixin ... failed injection check (0/1)`.

- Fabric `ClientPlayNetworkHandlerMixin`
  - Removed the obsolete injection into vanilla recipe synchronization.
  - PF-EMI 1.21.2 already synchronizes full recipe objects with `RecipeSyncS2CPacket`; the packet itself updates `EmiSyncedRecipes` and calls `EmiReloadManager.reloadRecipes()`.
  - This fixes the Fabric startup crash reporting `onSynchronizeRecipes ... failed injection check (0/1)`.

- `KeyboardMixin`
  - Proactively ported the same 1.21.2 input-path change from `Screen.wrapScreenError(...)` to the direct `Screen.keyPressed(int, int, int)` call.
  - Prevents the equivalent keyboard mixin failure from becoming the next runtime crash.

## Build script

The full source package keeps the latest clean-release `build.bat` naming behavior requested for PF-EMI builds.
