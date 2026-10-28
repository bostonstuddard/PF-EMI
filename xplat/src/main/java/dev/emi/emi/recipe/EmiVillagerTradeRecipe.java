package dev.emi.emi.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.item.trading.TradeCost;

/**
 * An informational offer template, not a guaranteed trade or a crafting recipe.
 * Prices, enchantments and trade availability depend on server state and RNG.
 */
public final class EmiVillagerTradeRecipe implements EmiRecipe {
																																			private final EmiRecipeCategory category;
																									private final Identifier id;
																																		private final List<EmiIngredient> costs;
																																private final EmiStack exampleOutput;
																						private final int level;
																															private final Component traderName;

																																																																							public EmiVillagerTradeRecipe(EmiRecipeCategory category, Identifier id, TradeCost first,
																																																																																																		@Nullable TradeCost second, EmiStack exampleOutput, int level, Component traderName) {
																											this.category = category;
																		this.id = id;
																																this.costs = new ArrayList<>();
																																			this.costs.add(displayCost(first));
																								if (second != null) {
																																							this.costs.add(displayCost(second));
									}
																																			this.exampleOutput = exampleOutput;
																							this.level = level;
																														this.traderName = traderName;
					}

																																																private static EmiIngredient displayCost(TradeCost cost) {
																				long amount = 1;
																																																						if (cost.count().value() instanceof ConstantValue constant) {
																																										amount = Math.max(1, constant.value());
									}
																																																				return EmiStack.of(cost.item().value()).setAmount(amount);
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
																		return costs;
					}

											@Override
																															public List<EmiStack> getOutputs() {
																															return List.of(exampleOutput);
					}

											@Override
																																public boolean supportsRecipeTree() {
																		return false;
					}

											@Override
																											public int getDisplayWidth() {
																	return 144;
					}

											@Override
																												public int getDisplayHeight() {
																return 48;
					}

											@Override
																																							public void addWidgets(WidgetHolder widgets) {
																																																		widgets.addSlot(costs.get(0), 3, 3).recipeContext(this);
																										if (costs.size() > 1) {
																																													widgets.addTexture(EmiTexture.PLUS, 24, 6);
																																																							widgets.addSlot(costs.get(1), 45, 3).recipeContext(this);
									}
																																														widgets.addTexture(EmiTexture.EMPTY_ARROW, 75, 3);
																																																					widgets.addSlot(exampleOutput, 107, 3).recipeContext(this);
																																										widgets.addText(traderName, 1, 23, -1, true);
																				if (level > 0) {
																																																																																	widgets.addText(EmiPort.translatable("emi.discovery.trade.level", level), 1, 32, -1, true);
									}
																																																																																					widgets.addTooltipText(List.of(EmiPort.translatable("emi.discovery.trade.variable")), 0, 22, 143, 26);
					}
}
