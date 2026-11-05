package dev.emi.emi.recipe;

import java.util.ArrayList;
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
		if (durable) {
			tools.add(cells.size() - 1);
			// ProjectFlint DurableCraftingRecipe also returns (and wears) Flint Files
			// and Bone Knives used as secondary ingredients.
			for (int i = 0; i < cells.size(); i++) {
				for (EmiStack candidate : cells.get(i).getEmiStacks()) {
					if (candidate.isEmpty() || candidate.getId() == null) continue;
					String name = candidate.getId().toString();
					if (name.equals("projectflint:flint_file") || name.equals("projectflint:bone_knife")) tools.add(i);
				}
			}
		}
		this.reusableToolIndices = Set.copyOf(tools);
	}

	@Override public EmiRecipeCategory getCategory() { return category; }
	@Override public Identifier getId() { return id; }
	@Override public List<EmiIngredient> getInputs() {
		List<EmiIngredient> items = new ArrayList<>();
		for (int i = 0; i < cells.size(); i++) {
			if (!reusableToolIndices.contains(i) && !cells.get(i).isEmpty()) items.add(cells.get(i));
		}
		return List.copyOf(items);
	}
	@Override public List<EmiIngredient> getCatalysts() {
		return reusableToolIndices.stream().sorted().map(cells::get).toList();
	}
	@Override public List<EmiStack> getOutputs() { return List.of(output); }
	@Override public int getDisplayWidth() { return gridSize * 18 + 64; }
	@Override public int getDisplayHeight() { return gridSize * 18 + (gridSize == 3 ? 16 : 0); }
	@Override public boolean supportsRecipeTree() { return false; }
	@Override public boolean hideCraftable() { return true; }

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
