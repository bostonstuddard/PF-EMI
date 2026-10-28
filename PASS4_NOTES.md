# PF-EMI 1.21.2 Compile Pass 4

Fixes the four remaining xplat compile errors in `InventoryRecipeHandler`.

Minecraft 1.21.2 moved crafting-grid dimensions onto `AbstractCraftingScreenHandler`:
- `getWidth()`
- `getHeight()`

`AbstractRecipeScreenHandler` no longer exposes the old `getCraftingWidth()` / `getCraftingHeight()` API.

Changes:
- `AbstractRecipeScreenHandler` type checks -> `AbstractCraftingScreenHandler`
- `getCraftingWidth()` -> `getWidth()`
- `getCraftingHeight()` -> `getHeight()`
