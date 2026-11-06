package dev.emi.emi.recipe;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * A real 2x2 or 3x3 grid tied to its ProjectFlint station.
 * Shaped entries preserve exact source JSON cell positions; shapeless entries
 * keep distinct tool/ingredient slots and do not claim to be machine recipes.
 */
public final class ProjectFlintGridRecipe implements EmiRecipe {
	private final EmiRecipeCategory category;
	private final Identifier id;
	private final List<EmiIngredient> cells;
	private final EmiStack output;
	private final int gridSize;
	private final boolean shapeless;
	private final Set<Integer> reusableToolIndices;

	public ProjectFlintGridRecipe(EmiRecipeCategory category, Identifier id,
		List<EmiIngredient> cells, EmiStack output, int gridSize, boolean shapeless) {
		this(category, id, cells, output, gridSize, shapeless, false);
	}

	public ProjectFlintGridRecipe(EmiRecipeCategory category, Identifier id,
		List<EmiIngredient> cells, EmiStack output, int gridSize, boolean shapeless, boolean durable) {
		if (gridSize != 2 && gridSize != 3) throw new IllegalArgumentException("Only 2x2 or 3x3 grids supported");
		if (cells.size() > gridSize * gridSize) throw new IllegalArgumentException("Too many grid cells");
		this.category = category;
		this.id = id;
		this.cells = List.copyOf(cells);
		this.output = output.copy();
		this.gridSize = gridSize;
		this.shapeless = shapeless;
		Set<Integer> tools = new HashSet<>();
		if (durable && !cells.isEmpty()) tools.add(cells.size() - 1);
		// Mark reusable tools for all station tiers. A tool is still part of the
		// transfer grid but should not be mistaken for a consumed ingredient.
		for (int i = 0; i < cells.size(); i++) {
			for (EmiStack candidate : cells.get(i).getEmiStacks()) {
				if (candidate.isEmpty() || candidate.getId() == null) continue;
				String name = candidate.getId().toString();
				if (name.equals("projectflint:flint_file")
					|| name.equals("projectflint:bone_knife")
					|| name.equals("projectflint:flint_plant_cutters")
					|| name.equals("minecraft:wooden_axe")
					|| name.equals("projectflint:hardwood_axe")
					|| name.equals("projectflint:unfired_brick_mold")) tools.add(i);
			}
		}
		this.reusableToolIndices = Set.copyOf(tools);
	}

	@Override public EmiRecipeCategory getCategory() { return category; }
	@Override public Identifier getId() { return id; }
	// EMI recipe transfer needs the COMPLETE crafting grid, including empty cells
	// and reusable tools. The former implementation dropped both and could not fill
	// shaped slots or find the tools already in the player inventory.
	@Override public List<EmiIngredient> getInputs() { return cells; }
	public int gridSize() { return gridSize; }
	public boolean isShapeless() { return shapeless; }
	@Override public List<EmiIngredient> getCatalysts() {
		return reusableToolIndices.stream().sorted().map(cells::get).toList();
	}
	@Override public List<EmiStack> getOutputs() { return List.of(output); }
	@Override public int getDisplayWidth() { return gridSize * 18 + 64; }
	@Override public int getDisplayHeight() { return gridSize * 18 + (gridSize == 3 ? 16 : 0); }
	@Override public boolean supportsRecipeTree() { return true; }
	@Override public boolean hideCraftable() { return false; }

	@Override public void addWidgets(WidgetHolder widgets) {
		for (int i = 0; i < gridSize * gridSize; i++) {
			EmiIngredient stack = i < cells.size() ? cells.get(i) : EmiStack.of(ItemStack.EMPTY);
			var slot = widgets.addSlot(stack, (i % gridSize) * 18, (i / gridSize) * 18)
				.catalyst(reusableToolIndices.contains(i));
			if (reusableToolIndices.contains(i)) {
				slot.appendTooltip(EmiPort.literal("Tool is reused; durability may decrease"));
			}
		}
		int gridPixels = gridSize * 18;
		widgets.addTexture(EmiTexture.EMPTY_ARROW, gridPixels + 3, (gridPixels - 17) / 2);
		var slot = widgets.addSlot(output, gridPixels + 31, (gridPixels - 18) / 2).recipeContext(this).large(true);
		if ("hardwood_crafting".equals(category.getId().getPath())) {
			slot.appendTooltip(EmiPort.literal("Crafting time: 3 seconds"));
		} else if ("personal_crafting".equals(category.getId().getPath())) {
			slot.appendTooltip(EmiPort.literal("Personal crafting (2x2)"));
		}
		// EMI's shapeless badge overlaps the result and looks like a second arrow.
		// Shapeless inputs are already represented by their unsorted grid positions.
		if (gridSize == 3) {
			widgets.addText(EmiPort.literal("Crafting time: 3 seconds"), 1, gridPixels + 4, 0xFF282828, false);
		}
	}
}
