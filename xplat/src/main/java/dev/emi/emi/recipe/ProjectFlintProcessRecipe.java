package dev.emi.emi.recipe;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.util.Identifier;

/**
 * Compact ProjectFlint process recipe with bounded multi-row inputs and outputs.
 */
public final class ProjectFlintProcessRecipe implements EmiRecipe {
	private final EmiRecipeCategory category;
	private final Identifier id;
	private final List<EmiIngredient> inputs;
	private final List<EmiIngredient> catalysts;
	private final List<EmiStack> outputs;
	private final int processTicks;
	private final int inputColumns;
	private final int inputRows;
	private final int outputColumns;
	private final int outputRows;
	private final int width;
	private final int height;

	public ProjectFlintProcessRecipe(
		EmiRecipeCategory category,
		Identifier id,
		List<EmiIngredient> inputs,
		List<EmiIngredient> catalysts,
		List<EmiStack> outputs,
		int processTicks
	) {
		this.category = category;
		this.id = id;
		this.inputs = List.copyOf(inputs);
		this.catalysts = List.copyOf(catalysts);
		this.outputs = List.copyOf(outputs);
		this.processTicks = Math.max(0, processTicks);

		int inputCount = Math.max(1, inputs.size() + catalysts.size());
		this.inputColumns = Math.min(4, inputCount);
		this.inputRows = (inputCount + inputColumns - 1) / inputColumns;

		int outputCount = Math.max(1, outputs.size());
		this.outputColumns = Math.min(2, outputCount);
		this.outputRows = (outputCount + outputColumns - 1) / outputColumns;

		int inputWidth = inputColumns * 18;
		int outputWidth = outputColumns * 18;
		this.width = Math.min(164, Math.max(94, inputWidth + 34 + outputWidth));
		this.height = Math.max(26, Math.max(inputRows * 18, outputRows * 18))
			+ (processTicks > 0 ? 10 : 0);
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return category;
	}

	@Override
	public Identifier getId() {
		return id;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return inputs;
	}

	@Override
	public List<EmiIngredient> getCatalysts() {
		return catalysts;
	}

	@Override
	public List<EmiStack> getOutputs() {
		return outputs;
	}

	@Override
	public int getDisplayWidth() {
		return width;
	}

	@Override
	public int getDisplayHeight() {
		return height;
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		List<DisplayIngredient> displayed = new ArrayList<>();
		for (EmiIngredient input : inputs) {
			displayed.add(new DisplayIngredient(input, false));
		}
		for (EmiIngredient catalyst : catalysts) {
			displayed.add(new DisplayIngredient(catalyst, true));
		}

		for (int i = 0; i < displayed.size(); i++) {
			DisplayIngredient ingredient = displayed.get(i);
			int x = (i % inputColumns) * 18;
			int y = (i / inputColumns) * 18;
			widgets.addSlot(ingredient.ingredient(), x, y)
				.catalyst(ingredient.catalyst());
		}

		int inputWidth = inputColumns * 18;
		int arrowY = Math.max(0, (Math.max(inputRows, outputRows) * 18 - 17) / 2);
		if (processTicks > 0) {
			widgets.addFillingArrow(inputWidth + 3, arrowY, processTicks * 50)
				.tooltip((mx, my) -> List.of(TooltipComponent.of(EmiPort.ordered(
					EmiPort.translatable("emi.cooking.time", processTicks / 20f)
				))));
		} else {
			widgets.addTexture(EmiTexture.EMPTY_ARROW, inputWidth + 3, arrowY);
		}

		int outputX = inputWidth + 32;
		int outputYOffset = Math.max(0, (inputRows * 18 - outputRows * 18) / 2);
		for (int i = 0; i < outputs.size(); i++) {
			int x = outputX + (i % outputColumns) * 18;
			int y = outputYOffset + (i / outputColumns) * 18;
			widgets.addSlot(outputs.get(i), x, y).recipeContext(this);
		}
	}

	private record DisplayIngredient(EmiIngredient ingredient, boolean catalyst) {
	}
}
