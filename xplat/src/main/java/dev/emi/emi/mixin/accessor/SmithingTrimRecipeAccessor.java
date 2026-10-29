package dev.emi.emi.mixin.accessor;

/**
 * Legacy source-path shim. Minecraft 1.21.2 exposes smithing recipe ingredients
 * through SmithingRecipe#template(), #base(), and #addition(), so PF-EMI no
 * longer needs a mixin accessor for SmithingTrimRecipe.
 */
@Deprecated(forRemoval = true)
final class SmithingTrimRecipeAccessor {
	private SmithingTrimRecipeAccessor() {
	}
}
