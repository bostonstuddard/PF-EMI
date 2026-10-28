package dev.emi.emi.recipe;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.util.Identifier;

/**
 * Informational PF recipe for world placement, block recovery and mob loot.
 *
 * Unlike normal crafting recipes, these pages can describe ranges/chances and
 * world-state changes without pretending that every displayed subject is a
 * consumed crafting ingredient.
 */
public final class ProjectFlintLootRecipe implements EmiRecipe {
	public record LootOutput(EmiStack stack, String note) {
		public LootOutput {
			stack = stack.copy();
		}
	}

	private final EmiRecipeCategory category;
	private final Identifier id;
	private final String title;
	private final EmiIngredient subject;
	private final boolean subjectIsInput;
	private final List<EmiIngredient> catalysts;
	private final List<LootOutput> outputs;
	private final List<String> notes;

	public ProjectFlintLootRecipe(
		EmiRecipeCategory category,
		Identifier id,
		String title,
		EmiIngredient subject,
		boolean subjectIsInput,
		List<EmiIngredient> catalysts,
		List<LootOutput> outputs,
		List<String> notes
	) {
		this.category = category;
		this.id = id;
		this.title = title;
		this.subject = subject.copy();
		this.subjectIsInput = subjectIsInput;
		this.catalysts = List.copyOf(catalysts);
		this.outputs = List.copyOf(outputs);
		this.notes = List.copyOf(notes);
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
		return subjectIsInput ? List.of(subject) : List.of();
	}

	@Override
	public List<EmiIngredient> getCatalysts() {
		// Mob spawn eggs are explanatory subjects, not consumed materials. Register
		// them as a non-consumed catalyst so EMI's Uses lookup can find their loot.
		if (subjectIsInput) return catalysts;
		List<EmiIngredient> searchable = new ArrayList<>(catalysts.size() + 1);
		searchable.add(subject);
		searchable.addAll(catalysts);
		return List.copyOf(searchable);
	}

	@Override
	public List<EmiStack> getOutputs() {
		return outputs.stream().map(LootOutput::stack).map(EmiStack::copy).toList();
	}

	@Override
	public int getDisplayWidth() {
		return 196;
	}

	@Override
	public int getDisplayHeight() {
		return notes.isEmpty() ? 54 : Math.min(82, 54 + notes.size() * 10);
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public boolean hideCraftable() {
		return true;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.add(new TextWidget(
			EmiPort.ordered(EmiPort.literal(title)),
			2,
			2,
			0xFFFFFFFF,
			true
		));

		int subjectY = 18;
		widgets.addSlot(subject, 2, subjectY)
			.catalyst(!subjectIsInput);

		int catalystX = 22;
		for (EmiIngredient catalyst : catalysts) {
			if (catalystX > 58) break;
			widgets.addSlot(catalyst, catalystX, subjectY).catalyst(true);
			catalystX += 19;
		}

		widgets.addTexture(EmiTexture.EMPTY_ARROW, 65, subjectY + 1);

		int outputX = 96;
		for (LootOutput output : outputs) {
			if (outputX + 18 > widgets.getWidth()) break;
			var slot = widgets.addSlot(output.stack(), outputX, subjectY).recipeContext(this);
			if (!output.note().isBlank()) {
				slot.appendTooltip(EmiPort.literal(output.note()));
			}
			outputX += 19;
		}

		int noteY = 42;
		for (String note : notes) {
			if (noteY + 9 > widgets.getHeight()) break;
			widgets.add(new TextWidget(
				EmiPort.ordered(EmiPort.literal(note)),
				2,
				noteY,
				0xFFB8B8B8,
				false
			));
			noteY += 10;
		}
	}
}
