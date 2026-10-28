package dev.emi.emi.recipe;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
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
			if (material.catalyst()) {
				continue;
			}
			EmiStack stack = material.stack().resolveExact(material.amount());
			if (!stack.isEmpty()) {
				inputs.add(stack);
			}
		}
		return inputs;
	}

	@Override
	public List<EmiIngredient> getCatalysts() {
		List<EmiIngredient> catalysts = new ArrayList<>();
		for (ProjectFlintMultiblockDefinition.Material material : definition.materials()) {
			if (!material.catalyst()) {
				continue;
			}
			EmiStack stack = material.stack().resolveExact(material.amount());
			if (!stack.isEmpty()) {
				catalysts.add(stack);
			}
		}
		return catalysts;
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
		widgets.add(new ProjectFlintMultiblockWidget(
			0,
			0,
			widgets.getWidth(),
			widgets.getHeight(),
			definition
		));
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
