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
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.item.trading.TradeCost;

/** A possible trade offer, not a promise about a specific server's trade pool. */
public final class EmiVillagerTradeRecipe implements EmiRecipe {
    private final EmiRecipeCategory category;
    private final Identifier id;
    private final List<EmiIngredient> costs;
    private final EmiStack output;
    private final int level;
    private final Component trader;

    public EmiVillagerTradeRecipe(EmiRecipeCategory category, Identifier id, TradeCost first,
            @Nullable TradeCost second, EmiStack output, int level, Component trader) {
        this.category = category;
        this.id = id;
        this.costs = new ArrayList<>();
        this.costs.add(displayCost(first));
        if (second != null) this.costs.add(displayCost(second));
        this.output = output;
        this.level = level;
        this.trader = trader;
    }

    private static EmiIngredient displayCost(TradeCost cost) {
        long amount = 1;
        if (cost.count() instanceof ConstantValue constant) {
            amount = Math.max(1, (long) constant.value());
        }
        return EmiStack.of(cost.item().value()).setAmount(amount);
    }

    @Override public EmiRecipeCategory getCategory() { return category; }
    @Override public Identifier getId() { return id; }
    @Override public List<EmiIngredient> getInputs() { return costs; }
    @Override public List<EmiStack> getOutputs() { return List.of(output); }
    @Override public boolean supportsRecipeTree() { return false; }
    @Override public int getDisplayWidth() { return 144; }
    @Override public int getDisplayHeight() { return 48; }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(costs.get(0), 3, 3).recipeContext(this);
        if (costs.size() > 1) {
            widgets.addTexture(EmiTexture.PLUS, 24, 6);
            widgets.addSlot(costs.get(1), 45, 3).recipeContext(this);
        }
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 75, 3);
        widgets.addSlot(output, 107, 3).recipeContext(this);
        widgets.addText(trader, 1, 23, -1, true);
        if (level > 0) {
            widgets.addText(EmiPort.literal("Villager level: " + level), 1, 32, -1, true);
        }
        widgets.addTooltipText(List.of(EmiPort.literal(
            "Potential trade: costs, output and availability may depend on server and trader")), 0, 22, 143, 26);
    }
}
