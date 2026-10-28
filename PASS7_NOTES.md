# PF-EMI 1.21.2 - Compile Pass 7

This pass targets the NeoForge-only compile errors reached after Fabric compiled successfully.

Changes:
- Removed the unavailable NeoForge `RecipesUpdatedEvent` client listener.
- Moved PF-EMI's authoritative recipe-object packet to `OnDatapackSyncEvent`, so recipes are sent on player join and again after server datapack reloads.
- Updated NeoForge `FluidIngredient` conversion from removed `getStacks()` to `fluids()` and converts the accepted fluid holders into display `FluidStack`s.
- Updated NeoForge fuel lookup to pass Minecraft 1.21.2's synchronized `FuelRegistry` into `ItemStack#getBurnTime`.

The complete Gradle compile cannot be executed in the artifact environment because external Gradle distribution resolution is DNS-blocked. Run `build.bat` locally to verify the next layer.
