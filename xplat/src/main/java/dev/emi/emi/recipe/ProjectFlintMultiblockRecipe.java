package dev.emi.emi.recipe;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.EmiPort;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.widget.ProjectFlintMultiblockWidget;
import net.minecraft.util.Identifier;

/** EMI recipe wrapper for the large GTNH-style ProjectFlint structure viewer. */
public final class ProjectFlintMultiblockRecipe implements EmiRecipe {
	private final EmiRecipeCategory category;
	private final ProjectFlintMultiblockDefinition definition;

	public ProjectFlintMultiblockRecipe(
		EmiRecipeCategory category,
		ProjectFlintMultiblockDefinition definition
	) {
		this.category = category;
		this.definition = definition;
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return category;
	}

	@Override
	public Identifier getId() {
		return definition.id();
	}

	@Override
	public List<EmiIngredient> getInputs() {
		List<EmiIngredient> inputs = new ArrayList<>();
		for (ProjectFlintMultiblockDefinition.Material material : definition.materials()) {
			if (!material.catalyst()) {
				EmiIngredient ingredient = materialIngredient(material);
				if (!ingredient.isEmpty()) inputs.add(ingredient);
			}
		}
		return inputs;
	}

	@Override
	public List<EmiIngredient> getCatalysts() {
		List<EmiIngredient> catalysts = new ArrayList<>();
		for (ProjectFlintMultiblockDefinition.Material material : definition.materials()) {
			if (material.catalyst()) {
				EmiIngredient ingredient = materialIngredient(material);
				if (!ingredient.isEmpty()) catalysts.add(ingredient);
			}
		}
		return catalysts;
	}

	/** Preserve alternate material variants in EMI lookups, not just the first display icon. */
	private static EmiIngredient materialIngredient(ProjectFlintMultiblockDefinition.Material material) {
		List<EmiStack> stacks = new ArrayList<>();
		for (Identifier id : material.stack().candidates()) {
			Item item = Registries.ITEM.get(id);
			if (item != null && item != Items.AIR && id.equals(Registries.ITEM.getId(item))) {
				stacks.add(EmiStack.of(item, material.amount()));
			}
		}
		return stacks.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(stacks, material.amount());
	}

	@Override
	public List<EmiStack> getOutputs() {
		return definition.outputs().stream()
			.map(ProjectFlintMultiblockDefinition.Output::stack)
			.map(EmiStack::copy)
			.toList();
	}

	@Override
	public int getDisplayWidth() {
		return 312;
	}

	@Override
	public int getDisplayHeight() {
		return 276;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		// Put the 3D canvas FIRST and EMI SlotWidgets AFTER it. This gives
		// the rails normal mouse/A/U behavior and native recipe/uses tooltips.
		widgets.add(new ProjectFlintMultiblockWidget(
			0, 0, widgets.getWidth(), widgets.getHeight(), definition
		));
		int railY = 31;
		for (ProjectFlintMultiblockDefinition.Material material : definition.materials()) {
			if (railY + 18 > widgets.getHeight() - 2) break;
			EmiIngredient ingredient = materialIngredient(material);
			if (ingredient.isEmpty()) ingredient = material.stack().resolveDisplay(material.amount());
			var slot = widgets.addSlot(ingredient, 7, railY)
				.catalyst(material.catalyst());
			slot.appendTooltip(EmiPort.literal(material.stack().label() + ": " + material.amount() + " needed"));
			if (!material.note().isBlank()) slot.appendTooltip(EmiPort.literal(material.note()));
			railY += 20;
		}
		railY = 31;
		for (ProjectFlintMultiblockDefinition.Output output : definition.outputs()) {
			if (railY + 18 > widgets.getHeight() - 2) break;
			var slot = widgets.addSlot(output.stack(), widgets.getWidth() - 26, railY)
				.recipeContext(this);
			if (!output.note().isBlank()) slot.appendTooltip(EmiPort.literal(output.note()));
			railY += 20;
		}
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public boolean hideCraftable() {
		return true;
	}
}
