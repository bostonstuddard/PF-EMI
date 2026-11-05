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

	// No free-floating text: each note is wrapped to fit a conservative 192px line.
	private static List<String> wrap(String text) {
		List<String> result = new ArrayList<>();
		for (String paragraph : text.split("\\n", -1)) {
			StringBuilder line = new StringBuilder();
			for (String word : paragraph.trim().split("\\s+")) {
				if (line.length() > 0 && line.length() + 1 + word.length() > 30) {
					result.add(line.toString());
					line.setLength(0);
				}
				if (word.length() > 30) {
					if (line.length() > 0) { result.add(line.toString()); line.setLength(0); }
					for (int i = 0; i < word.length(); i += 30) result.add(word.substring(i, Math.min(word.length(), i + 30)));
				} else {
					if (line.length() > 0) line.append(' ');
					line.append(word);
				}
			}
			if (line.length() > 0) result.add(line.toString());
		}
		return result;
	}

	@Override
	public int getDisplayWidth() { return 210; }

	@Override
	public int getDisplayHeight() {
		return Math.max(54, 46 + notes.stream().mapToInt(note -> wrap(note).size()).sum() * 11);
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
			0xFF242424,
			false
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

		if (!outputs.isEmpty()) widgets.addTexture(EmiTexture.EMPTY_ARROW, 65, subjectY + 1);

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
			for (String line : wrap(note)) {
				widgets.add(new TextWidget(
					EmiPort.ordered(EmiPort.literal(line)), 2, noteY, 0xFF303030, false
				));
				noteY += 11;
			}
		}
	}
}
