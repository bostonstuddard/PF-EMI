package dev.emi.emi;

import java.util.HashSet;
import java.util.Set;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Recipe-driven PF-EMI index visibility.
 *
 * ProjectFlint intentionally owns its progression catalog. Minecraft and
 * ProjectFlint item stacks are only left in EMI if they participate in one of
 * the production/use routes that PF-EMI actually exposes.
 */
public final class ProjectFlintRecipeIndex {
	private static final Set<Identifier> VISIBLE_ITEMS = new HashSet<>();

	private ProjectFlintRecipeIndex() {
	}

	public static void reset() {
		VISIBLE_ITEMS.clear();
	}

	public static void mark(EmiIngredient ingredient) {
		if (ingredient == null) {
			return;
		}
		for (EmiStack stack : ingredient.getEmiStacks()) {
			mark(stack);
		}
	}

	public static void mark(EmiStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		if (stack.getKey() instanceof Item item) {
			Identifier id = Registries.ITEM.getId(item);
			// Spawn eggs referenced by the mob-loot guide stay searchable as non-consumed catalysts.
			VISIBLE_ITEMS.add(id);
		}
	}

	public static void mark(EmiRecipe recipe) {
		for (EmiIngredient input : recipe.getInputs()) {
			mark(input);
		}
		for (EmiIngredient catalyst : recipe.getCatalysts()) {
			mark(catalyst);
		}
		for (EmiStack output : recipe.getOutputs()) {
			mark(output);
		}
	}

	public static boolean shouldHide(EmiStack stack) {
		if (!(stack.getKey() instanceof Item item)) {
			return false;
		}
		Identifier id = Registries.ITEM.getId(item);
		String namespace = id.getNamespace();
		if (!"minecraft".equals(namespace) && !"projectflint".equals(namespace)) {
			return false;
		}
		// Avoid silently concealing new ProjectFlint items when the gameplay mod
		// introduces recipes or interactions ahead of this compatibility catalog.
		if ("projectflint".equals(namespace)) return false;
		return !VISIBLE_ITEMS.contains(id);
	}
}
