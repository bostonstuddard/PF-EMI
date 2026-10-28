# PF-EMI 1.21.2 Port Status

Target: Minecraft **1.21.2**, Java **21**, Fabric + NeoForge.

This source tree contains the actual PF-EMI 1.21.2 port code, not just metadata changes.

## Port work included

- Targeted Minecraft 1.21.2 / Yarn 1.21.2 mappings and 1.21.2 loader/API versions.
- Preserved the runtime mod id `emi` for EMI API/mod compatibility while keeping PF-EMI branding.
- Added 1.21.2 recipe synchronization with `RecipeEntry.PACKET_CODEC` so the client has complete recipes for EMI indexing.
- Added a client-side synced-recipe index used by EMI's recipe handlers on 1.21.2.
- Migrated 1.21.2 Ingredient APIs (`getMatchingItems`, placement ingredients, Optional shaped ingredients).
- Migrated recipe result handling to 1.21.2 recipe displays / slot display contexts.
- Added EMI support for Minecraft 1.21.2 `TransmuteRecipe`.
- Migrated repair-material handling to the 1.21.2 `REPAIRABLE` data component.
- Added a DrawContext compatibility helper for the 1.21.2 RenderLayer-based texture API.
- Updated Fabric and NeoForge networking to carry PF-EMI's recipe sync packet.
- Disabled the optional JEMI compilation path for this target; the core EMI/PF-EMI API and recipe viewer remain present.
- Added simple Windows (`build.bat`) and Unix (`build.sh`) build launchers.

## Validation performed in this workspace

- JSON resources parse successfully.
- Java source brace/static structure checks pass for the Fabric, NeoForge, and shared source sets.
- `git diff --check` is used before packaging.

A complete Gradle compile could not be run in this execution environment because the Gradle wrapper distribution/dependencies cannot be resolved from the network here (`UnknownHostException` while accessing `services.gradle.org`). Run `build.bat` on Windows or `./build.sh` on an internet-connected development machine with Java 21 to perform the full compiler/runtime verification.

## Build toolchain pin

The Gradle plugins are intentionally pinned instead of using mutable `SNAPSHOT` versions:

- Architectury Plugin: `3.4.162`
- Architectury Loom: `1.11.458`
- Gradle Wrapper: `8.14`
- Java: `21`

This prevents current snapshot publications compiled with newer Java versions from breaking the 1.21.2 build with errors such as `Unsupported class file major version 69`.

## Gradle Java runtime guard

`build.bat` and `build.sh` now resolve `java.home` from the Java runtime actually found in `PATH`, then force both the Gradle wrapper and Gradle daemon to use that same JDK. This prevents a machine-level `JAVA_HOME` or user Gradle property from silently launching Gradle 8.14 under Java 25 (`class file major version 69`) while the preliminary `java -version` command reports Java 21.


## 1.21.2 compile pass 2

Fixed the first real 1.21.2 Yarn compile batch reported after Gradle reached `:xplat:compileJava`:

- moved `ModelTransformationMode` imports to `net.minecraft.item`
- corrected `RecipeInput` imports to `net.minecraft.recipe.input`
- removed stale/unused `ComponentMapImpl` and `SuspiciousStewItem` imports
- updated the private `DrawContext.drawTooltip` invoker for the new texture parameter
- removed obsolete smithing-field accessor mixins and use the public 1.21.2 smithing recipe API
- ported `AbstractInventoryScreenMixin` to the new 1.21.2 `StatusEffectsDisplay` owner
- updated the vanilla exclusion-area screen checks for `InventoryScreen` and `CreativeInventoryScreen`

This environment cannot resolve Gradle/Maven hosts, so the next compiler pass must still be run locally.

## Runtime mixin pass 8 (2026-09-13)

After both Fabric and NeoForge compiled, first-launch testing exposed stale 1.21.1-era mixin targets:

- NeoForge: `MouseMixin` still targeted `Screen.wrapScreenError(...)`, removed from the 1.21.2 mouse path.
- Fabric: `ClientPlayNetworkHandlerMixin` still injected into the old vanilla recipe-manager mutation path even though PF-EMI 1.21.2 now owns full recipe synchronization with `RecipeSyncS2CPacket`.
- Keyboard input used the same removed `Screen.wrapScreenError(...)` pattern and was proactively ported before it could become the next startup failure.

Pass 8 retargets mouse/keyboard hooks to the direct 1.21.2 `Screen` input methods and removes the obsolete Fabric vanilla recipe-sync injection.
