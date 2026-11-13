package dev.emi.emi.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import com.google.common.collect.Lists;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiLog;

public class EmiShapedRecipe extends EmiCraftingRecipe {

	public EmiShapedRecipe(ShapedRecipe recipe) {
		super(padIngredients(recipe), EmiStack.of(EmiPort.getOutput(recipe)), EmiPort.getId(recipe), false);
		setRemainders(input, recipe);
	}

	public static void setRemainders(List<EmiIngredient> input, CraftingRecipe recipe) {
		// Custom shaped recipes can expose compressed placement ingredients; use
		// their actual recipe display to restore 3x3 positions and components.
		restoreShapedDisplay(input, recipe);
		try {
			TransientCraftingContainer inv = EmiUtil.getCraftingInventory();
			for (int i = 0; i < input.size(); i++) {
				if (input.get(i).isEmpty()) {
					continue;
				}
				for (int j = 0; j < input.size(); j++) {
					if (j == i) {
						continue;
					}
					if (!input.get(j).isEmpty()) {
						inv.setItem(j, input.get(j).getEmiStacks().get(0).getItemStack().copy());
					}
				}
				List<EmiStack> stacks = input.get(i).getEmiStacks();
				for (EmiStack stack : stacks) {
					inv.setItem(i, stack.getItemStack().copy());
					CraftingInput cri = CraftingInput.of(inv.getWidth(), inv.getHeight(), inv.getItems());
					if (cri.width() <= 3 && cri.height() <= 3) {
						ItemStack remainder = recipe.getRemainingItems(cri).get((i / 3 * cri.width()) + (i % 3));
						if (!remainder.isEmpty()) {
							stack.setRemainder(EmiStack.of(remainder));
						}
					}
				}
				inv.clearContent();
			}
		} catch (Exception e) {
			EmiLog.error("Exception thrown setting remainders for " + EmiPort.getId(recipe), e);
		}
	}

	/** Restore component-sensitive ingredients and the actual 3x3 arrangement. */
	private static void restoreShapedDisplay(List<EmiIngredient> input, CraftingRecipe recipe) {
		// Shapeless recipes use immutable lists; do not rewrite their layout.
		if (!(input instanceof ArrayList)) {
			return;
		}
		try {
			ShapedCraftingRecipeDisplay shaped = recipe.display().stream()
				.filter(ShapedCraftingRecipeDisplay.class::isInstance)
				.map(ShapedCraftingRecipeDisplay.class::cast)
				.filter(d -> d.width() > 0 && d.width() <= 3 && d.height() > 0 && d.height() <= 3)
				.findFirst().orElse(null);
			if (shaped == null) {
				return;
			}

			int width = shaped.width();
			int height = shaped.height();
			PlacementInfo placement = recipe.placementInfo();
			if (placement.slotsToIngredientIndex().size() != width * height) {
				return;
			}

			ContextMap context = Minecraft.getInstance().level != null
				? SlotDisplayContext.fromLevel(Minecraft.getInstance().level)
				: new ContextMap.Builder().create(SlotDisplayContext.CONTEXT);
			List<EmiIngredient> rebuilt = new ArrayList<>(9);
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					if (x >= width || y >= height) {
						rebuilt.add(EmiStack.EMPTY);
						continue;
					}
					int slot = x + y * width;
					int ingredientIndex = placement.slotsToIngredientIndex().getInt(slot);
					SlotDisplay display = shaped.ingredients().get(slot);
					if (ingredientIndex == PlacementInfo.EMPTY_SLOT || display instanceof SlotDisplay.Empty) {
						rebuilt.add(EmiStack.EMPTY);
						continue;
					}

					int fallbackIndex = recipe instanceof ShapedRecipe ? x + y * 3 : ingredientIndex;
					EmiIngredient fallback = fallbackIndex >= 0 && fallbackIndex < input.size()
						? input.get(fallbackIndex) : EmiStack.EMPTY;
					rebuilt.add(resolveDisplayIngredient(display, context, fallback));
				}
			}

			input.clear();
			input.addAll(rebuilt);
		} catch (Exception e) {
			EmiLog.error("Could not restore shaped recipe display for " + EmiPort.getId(recipe), e);
		}
	}

	private static EmiIngredient resolveDisplayIngredient(SlotDisplay display, ContextMap context, EmiIngredient fallback) {
		if (display instanceof SlotDisplay.TagSlotDisplay) {
			return fallback;
		}
		try {
			List<EmiStack> alternatives = display.resolveForStacks(context).stream()
				.filter(stack -> !stack.isEmpty())
				.map(EmiStack::of)
				.toList();
			if (alternatives.isEmpty()) {
				return fallback;
			}
			if (alternatives.size() == 1) {
				return alternatives.get(0);
			}
			if (alternatives.stream().anyMatch(stack -> !stack.getComponentChanges().isEmpty())) {
				return new ListEmiIngredient(alternatives, 1);
			}
			return EmiTags.getIngredient(net.minecraft.world.item.Item.class, alternatives, 1);
		} catch (Exception e) {
			return fallback;
		}
	}

	private static List<EmiIngredient> padIngredients(ShapedRecipe recipe) {
		List<EmiIngredient> list = Lists.newArrayList();
		int i = 0;
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				if (x >= recipe.getWidth() || y >= recipe.getHeight() || i >= recipe.getIngredients().size()) {
					list.add(EmiStack.EMPTY);
				} else {
					Optional<Ingredient> opt = recipe.getIngredients().get(i++);
					if (opt.isPresent()) {
						list.add(EmiIngredient.of(opt.get()));
					} else {
						list.add(EmiStack.EMPTY);
					}
				}
			}
		}
		return list;
	}
}
