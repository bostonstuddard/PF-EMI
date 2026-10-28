package dev.emi.emi.config;

import dev.emi.emi.EmiPort;
import net.minecraft.network.chat.Component;

/** Configurable location for the existing EMI and recipe-tree buttons. */
public enum EmiButtonAnchor implements ConfigEnum {
    BOTTOM_LEFT("bottom-left"),
    SEARCH_LEFT("search-left"),
    SEARCH_RIGHT("search-right"),
    TOP_LEFT("top-left"),
    TOP_RIGHT("top-right");

    private final String name;

    EmiButtonAnchor(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Component getText() {
        return EmiPort.translatable("emi.button_anchor." + name);
    }
}
