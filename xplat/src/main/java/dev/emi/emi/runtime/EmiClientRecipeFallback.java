package dev.emi.emi.runtime;

import dev.emi.emi.platform.EmiAgnos;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeMap;

/**
 * Client-only, read-only recipe viewer fallback. This never changes the server's
 * crafting rules, known recipes, network packets, or registry contents.
 */
public final class EmiClientRecipeFallback {
    // Allow time for recipe and tag packets, including on slow servers.
    private static final int GRACE_TICKS = 200;

    private static volatile RecipeMap fallbackMap;
    private static ClientPacketListener connection;
    private static int ticksMissing;
    private static boolean attempted;

    private EmiClientRecipeFallback() { }

    /** Invoked on the Minecraft client thread after a normal game tick. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener live = mc.getConnection();
        if (live == null || mc.level == null || mc.getSingleplayerServer() != null) {
            reset();
            return;
        }

        if (connection != live) {
            reset();
            connection = live;
        }

        // An actual server map, even an intentionally empty one, wins.
        // NEVER treat a non-null empty map as a missing synchronization.
        if (EmiAgnos.getRecipeMap() != null) {
            ticksMissing = 0;
            if (fallbackMap != null) {
                fallbackMap = null;
                EmiLog.info("PF-EMI: server recipes received; removed client vanilla fallback");
                EmiReloadManager.reload();
            }
            return;
        }

        if (attempted || fallbackMap != null || ++ticksMissing < GRACE_TICKS) {
            return;
        }

        attempted = true;
        RecipeMap loaded = VanillaClientRecipeLoader.load(mc.level.registryAccess());

        // A server may synchronize while the built-in recipes are being parsed.
        if (connection != live || mc.level == null || EmiAgnos.getRecipeMap() != null) {
            return;
        }
        if (loaded == null || loaded.values().isEmpty()) {
            EmiLog.warn("PF-EMI: server recipe map missing and bundled vanilla fallback unavailable");
            return;
        }
        fallbackMap = loaded;
        EmiLog.warn("PF-EMI: showing bundled vanilla recipes for viewing only (server recipes were not provided)");
        if (mc.player != null) {
            mc.gui.getChat().addClientSystemMessage(Component.literal(
                "[PF-EMI] Server recipes unavailable; showing vanilla recipes for viewing only. These may differ from the server."
            ).withStyle(ChatFormatting.YELLOW));
        }
        EmiReloadManager.reload();
    }

    /** No network or gameplay code should use this map as authoritative. */
    public static RecipeMap getFallbackMap() {
        return fallbackMap;
    }

    public static boolean isUsingFallback() {
        return fallbackMap != null && EmiAgnos.getRecipeMap() == null;
    }

    /** Reset before a new world/server connection or on disconnect. */
    public static void reset() {
        fallbackMap = null;
        connection = null;
        ticksMissing = 0;
        attempted = false;
    }
}
