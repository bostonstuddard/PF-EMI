package dev.emi.emi.handler;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Ingredient-transfer support for known ProjectFlint machine screens.
 * The machine remains authoritative for processing, fuel, and output timing.
 * We intentionally do not auto-collect result slots.
 */
public final class ProjectFlintStationRecipeHandler implements StandardRecipeHandler<ScreenHandler> {
    private final String categoryId;
    private final int[] inputSlotIds;

    public ProjectFlintStationRecipeHandler(String categoryId, int... inputSlotIds) {
        this.categoryId = "projectflint:" + categoryId;
        this.inputSlotIds = inputSlotIds.clone();
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.supportsRecipeTree() && recipe.getCategory() != null
            && recipe.getCategory().getId() != null
            && categoryId.equals(recipe.getCategory().getId().toString())
            && !recipe.getInputs().isEmpty() && recipe.getInputs().size() <= inputSlotIds.length;
    }

    @Override
    public List<Slot> getCraftingSlots(ScreenHandler handler) {
        List<Slot> slots = new ArrayList<>();
        for (int slotId : inputSlotIds) slots.add(handler.getSlot(slotId));
        return slots;
    }

    @Override
    public List<Slot> getInputSources(ScreenHandler handler) {
        List<Slot> sources = new ArrayList<>(getCraftingSlots(handler));
        var player = MinecraftClient.getInstance().player;
        if (player == null) return sources;
        for (Slot slot : handler.slots) {
            // Real player inventory only: never pull ghost preview items or recipe outputs.
            if (slot.inventory == player.getInventory() && slot.isEnabled()) sources.add(slot);
        }
        return sources;
    }
}
