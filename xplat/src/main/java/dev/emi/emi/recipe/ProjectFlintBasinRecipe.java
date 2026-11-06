package dev.emi.emi.recipe;

import java.util.List;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.util.Identifier;

/** Small, player-facing Basin recipe. Each result slot represents one actual roll. */
public final class ProjectFlintBasinRecipe implements EmiRecipe {
	private final EmiRecipeCategory category;
	private final Identifier id;
	private final EmiIngredient input;
	private final EmiIngredient basin;
	private final List<ProjectFlintLootRecipe.LootOutput> results;
	private final int stomps;

	public ProjectFlintBasinRecipe(EmiRecipeCategory category, Identifier id,
		EmiIngredient input, EmiIngredient basin,
		List<ProjectFlintLootRecipe.LootOutput> results, int stomps) {
		this.category = category;
		this.id = id;
		this.input = input.copy();
		this.basin = basin.copy();
		this.results = List.copyOf(results);
		this.stomps = stomps;
	}

	@Override public EmiRecipeCategory getCategory() { return category; }
	@Override public Identifier getId() { return id; }
	@Override public List<EmiIngredient> getInputs() { return List.of(input); }
	@Override public List<EmiIngredient> getCatalysts() { return List.of(basin); }
	@Override public List<EmiStack> getOutputs() { return results.stream().map(r -> r.stack().copy()).toList(); }
	@Override public int getDisplayWidth() { return 155; }
	@Override public int getDisplayHeight() { return results.size() > 3 ? 65 : 47; }
	@Override public boolean supportsRecipeTree() { return !input.isEmpty() && !results.isEmpty(); }
	@Override public boolean hideCraftable() { return true; }

	@Override
	public void addWidgets(WidgetHolder widgets) {
		// One short rule; item slots contain the exact output chances on hover.
		widgets.addText(EmiPort.literal(stomps + " stomps | 1 food each"),
			4, 2, 0xFF2A2A2A, false);
		widgets.addSlot(input, 4, 21);
		widgets.addSlot(basin, 24, 21).catalyst(true)
			.appendTooltip(EmiPort.literal("Holds up to 4 inputs; processes oldest first"));
		widgets.addTexture(EmiTexture.EMPTY_ARROW, 51, 22);
		for (int i = 0; i < results.size(); i++) {
			ProjectFlintLootRecipe.LootOutput result = results.get(i);
			int x = 85 + (i % 3) * 19;
			int y = 21 + (i / 3) * 19;
			var slot = widgets.addSlot(result.stack(), x, y).recipeContext(this);
			if (!result.note().isEmpty()) slot.appendTooltip(EmiPort.literal(result.note()));
		}
	}
}
