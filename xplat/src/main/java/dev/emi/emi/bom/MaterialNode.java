package dev.emi.emi.bom;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiPartialConsumptionRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

public class MaterialNode {
	public final EmiIngredient ingredient;
	public final EmiStack remainder;
	public @Nullable EmiRecipe recipe;
	public @Nullable List<MaterialNode> children;
	public float consumeChance = 1, produceChance = 1;
	public long amount = 1;
	public long divisor = 1;
	public long remainderAmount = 0;
	/** Opt-in units used by this input per parent craft, and capacity per physical item. */
	public long partialUnitsPerCraft = 0;
	public long partialCapacityUnits = 0;
	public boolean catalyst = false;
	// Should these be decoupled from material nodes?
	public FoldState state = FoldState.EXPANDED;
	public ProgressState progress = ProgressState.UNSTARTED;
	public long neededBatches = 0, totalNeeded = 0;

	public MaterialNode(EmiIngredient ingredient) {
		this.amount = ingredient.getAmount();
		this.ingredient = ingredient.copy().setAmount(1).setChance(1);
		if (this.ingredient.getEmiStacks().size() == 1) {
			this.remainder = this.ingredient.getEmiStacks().get(0).getRemainder();
			this.remainderAmount = remainder.getAmount();
			this.remainder.setAmount(1);
		} else {
			this.remainder = EmiStack.EMPTY;
		}
		out: {
			for (EmiStack stack : ingredient.getEmiStacks()) {
				if (!stack.getRemainder().equals(stack)) {
					break out;
				}
			}
			catalyst = true;
		}
	}

	public MaterialNode(MaterialNode node) {
		this.ingredient = node.ingredient;
		this.remainder = node.remainder;
		this.recipe = node.recipe;
		this.amount = node.amount;
		this.divisor = node.divisor;
		this.remainderAmount = node.remainderAmount;
		this.partialUnitsPerCraft = node.partialUnitsPerCraft;
		this.partialCapacityUnits = node.partialCapacityUnits;
	}

	public void recalculate(MaterialTree tree) {
		recalculate(tree, Lists.newArrayList());
	}

	private void recalculate(MaterialTree tree, List<EmiRecipe> used) {
		EmiRecipe recipe = this.recipe;
		if (!used.isEmpty()) {
			recipe = tree.getRecipe(ingredient);
		}
		if (recipe != null) {
			if (used.contains(recipe)) {
				return;
			}
			used.add(recipe);
			defineRecipe(recipe);
			for (MaterialNode node : children) {
				node.recalculate(tree, used);
			}
			used.remove(used.size() - 1);
		}
	}

	public void defineRecipe(EmiRecipe recipe) {
		produceChance = 1;
		if (recipe == null) {
			return;
		}
		this.recipe = recipe;
		divisor = 0;
		for (EmiStack stack : recipe.getOutputs()) {
			if (stack.equals(ingredient)) {
				if (divisor > 0) {
					if (produceChance != 1 || stack.getChance() != 1) {
						produceChance = (stack.getAmount() * stack.getChance() + divisor * produceChance) / (divisor + stack.getAmount());
					}
					divisor += stack.getAmount();
				} else {
					divisor = stack.getAmount();
					produceChance = stack.getChance();
				}
			}
		}
		if (divisor <= 0) {
			divisor = 1;
		}
		this.children = Lists.newArrayList();
		List<EmiIngredient> inputs = recipe.getInputs();
		List<EmiPartialConsumptionRecipe.PartialInput> partials = recipe instanceof EmiPartialConsumptionRecipe partial
			? partial.getPartialInputs() : List.of();
		outer:
		for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++) {
			EmiIngredient i = inputs.get(inputIndex);
			long capacity = 0;
			long used = 0;
			for (EmiPartialConsumptionRecipe.PartialInput partial : partials) {
				if (partial.inputIndex() == inputIndex) {
					capacity = partial.capacityUnits();
					used = partial.usedUnitsPerCraft();
					break;
				}
			}
			EmiStack remainder = EmiStack.EMPTY;
			if (i.getEmiStacks().size() == 1) {
				remainder = i.getEmiStacks().get(0).getRemainder();
			}
			for (MaterialNode node : children) {
				if (EmiIngredient.areEqual(i, node.ingredient) && EmiIngredient.areEqual(remainder, node.remainder)
						&& node.partialCapacityUnits == capacity && (capacity > 0 || node.partialUnitsPerCraft == 0)) {
					node.amount += i.getAmount();
					node.remainderAmount += remainder.getAmount();
					if (capacity > 0) {
						node.partialUnitsPerCraft = Long.MAX_VALUE - node.partialUnitsPerCraft < used
							? Long.MAX_VALUE : node.partialUnitsPerCraft + used;
					}
					continue outer;
				}
			}
			if (!i.isEmpty()) {
				MaterialNode node = new MaterialNode(i);
				node.partialCapacityUnits = capacity;
				node.partialUnitsPerCraft = used;
				if (capacity > 0) {
					// These are consumable units, not a normal reusable catalyst.
					node.catalyst = false;
				}
				node.consumeChance = i.getChance();
				children.add(node);
			}
		}
	}
}
