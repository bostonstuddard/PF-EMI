package dev.emi.emi.config;

import dev.emi.emi.EmiPort;
import net.minecraft.network.chat.Component;

/** Search-based highlights in handled inventories; the default preserves EMI's original dimming. */
public enum InventoryHighlightStyle implements ConfigEnum {
    DIM("dim"),
    OUTLINE("outline"),
    DIM_AND_OUTLINE("dim-and-outline");

    private final String name;

    InventoryHighlightStyle(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Component getText() {
        return EmiPort.translatable("emi.inventory_highlight." + name);
    }
}
