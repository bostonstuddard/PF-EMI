package dev.emi.emi.runtime;

import dev.emi.emi.platform.EmiAgnos;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeMap;

/**
 * Read-only client default recipes for servers that do not sync a recipe map.
 * The server's real map always takes precedence, including an intentionally
 * empty map. Fallback state is scoped to a single connection.
 */
public final class EmiClientRecipeFallback {
    // Wait briefly for server sync. If recipes arrive later, they always replace
    // this client-only display fallback, including intentionally empty maps.
    private static final int GRACE_TICKS = 60;
    private static volatile RecipeMap fallbackMap;
    private static ClientPacketListener connection;
    private static int ticksWithoutRecipes;
    private static boolean attempted;

    private EmiClientRecipeFallback() {}

    /** Called on the Minecraft client thread once per client tick. */
    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        ClientPacketListener current = client.getConnection();
        if (current == null || client.level == null || client.getSingleplayerServer() != null) {
            reset();
            return;
        }
        if (current != connection) {
            connection = current;
            fallbackMap = null;
            ticksWithoutRecipes = 0;
            attempted = false;
        }

        // An explicit server map, even an empty one, is authoritative.
        if (EmiAgnos.getRecipeMap() != null) {
            ticksWithoutRecipes = 0;
            if (fallbackMap != null) {
                fallbackMap = null;
                EmiLog.info("Server recipes received; replacing client-default recipe fallback");
                EmiReloadManager.reload();
            }
            return;
        }
        if (attempted || fallbackMap != null || ++ticksWithoutRecipes < GRACE_TICKS) {
            return;
        }
        attempted = true;
        RecipeMap map = VanillaClientRecipeLoader.load(client.level.registryAccess());
        // Server state or connection can change while client recipes decode.
        if (connection != current || client.level == null || EmiAgnos.getRecipeMap() != null) {
            return;
        }
        if (map == null || map.values().isEmpty()) {
            EmiLog.warn("Server sent no usable recipe map and client vanilla fallback could not be loaded");
            return;
        }
        fallbackMap = map;
        EmiLog.warn("Using client-default Minecraft recipes: these may differ from server recipes");
        if (client.player != null) {
            client.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("[PF-EMI] Server recipes unavailable; showing vanilla recipes for viewing only. These may differ from the server.")
                            .withStyle(ChatFormatting.YELLOW));
        }
        EmiReloadManager.reload();
    }

    public static RecipeMap getFallbackMap() {
        return fallbackMap;
    }

    public static boolean isUsingFallback() {
        return fallbackMap != null && EmiAgnos.getRecipeMap() == null;
    }

    public static void reset() {
        fallbackMap = null;
        connection = null;
        ticksWithoutRecipes = 0;
        attempted = false;
    }
}
