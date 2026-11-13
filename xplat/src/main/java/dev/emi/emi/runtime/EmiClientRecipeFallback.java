package dev.emi.emi.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.emi.emi.EmiSyncedRecipeMaps;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.platform.EmiClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

/**
 * Read-only vanilla recipe browser on remote servers without the EMI protocol.
 * The server's recipes override matching bundled vanilla IDs. A server can
 * legitimately send an empty or incomplete recipe book; that does not mean it
 * contains all vanilla recipes. Never change game-side recipe permissions.
 */
public final class EmiClientRecipeFallback {
    private static final int GRACE_TICKS = 60;
    private static volatile RecipeMap fallbackMap;
    private static RecipeMap vanillaMap;
    private static RecipeMap lastServerMap;
    private static ClientPacketListener connection;
    private static int ticksWithoutRecipes;
    private static boolean attempted;

    private EmiClientRecipeFallback() {}

    /** Runs on the Minecraft client thread. */
    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        ClientPacketListener current = client.getConnection();
        if (current == null || client.level == null || client.getSingleplayerServer() != null) {
            reset();
            return;
        }
        if (current != connection) {
            reset();
            connection = current;
        }

        // A genuine EMI server can synchronize its complete recipe set. Never
        // overlay bundled vanilla recipes on that explicitly modded protocol.
        if (EmiClient.onServer) {
            if (fallbackMap != null) {
                fallbackMap = null;
                vanillaMap = null;
                VanillaRecipeTags.clear();
                EmiLog.info("EMI server detected; switched back to server-authoritative recipes");
                EmiReloadManager.reload();
            }
            return;
        }

        RecipeMap server = EmiAgnos.getRecipeMap();
        if (vanillaMap != null) {
            // Recipe-sync callbacks can arrive after fallback loads (or during
            // a server-side reload). Rebuild once per changed map, not per tick.
            if (lastServerMap != server) {
                lastServerMap = server;
                fallbackMap = combine(vanillaMap, server);
                EmiLog.info("Refreshed client vanilla recipe display with updated server recipes");
                EmiReloadManager.reload();
            }
            return;
        }
        if (attempted || ++ticksWithoutRecipes < GRACE_TICKS) {
            return;
        }
        attempted = true;
        RecipeMap vanilla = VanillaClientRecipeLoader.load(client.level.registryAccess());
        if (connection != current || client.level == null || EmiClient.onServer) {
            return;
        }
        if (vanilla == null || vanilla.values().isEmpty()) {
            VanillaRecipeTags.clear();
            EmiLog.warn("Unable to load bundled vanilla recipes for this remote server");
            return;
        }

        vanillaMap = vanilla;
        lastServerMap = EmiAgnos.getRecipeMap();
        fallbackMap = combine(vanilla, lastServerMap);
        EmiLog.warn("Using bundled vanilla recipe browser; server recipes take precedence, but unavailable server data may differ");
        if (client.player != null) {
            client.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("[PF-EMI] Using vanilla recipe references; server-specific recipes or restrictions may differ.")
                            .withStyle(ChatFormatting.YELLOW));
        }
        EmiReloadManager.reload();
    }

    private static RecipeMap combine(RecipeMap vanilla, RecipeMap server) {
        if (server == null || server.values().isEmpty()) {
            return vanilla;
        }
        Map<Identifier, RecipeHolder<?>> combined = new LinkedHashMap<>();
        for (RecipeHolder<?> holder : vanilla.values()) {
            combined.put(holder.id().identifier(), holder);
        }
        for (RecipeHolder<?> holder : server.values()) {
            combined.put(holder.id().identifier(), holder);
        }
        EmiLog.info("Serverless browser: " + vanilla.values().size() + " vanilla recipes, "
                + server.values().size() + " server recipes, " + combined.size() + " combined; server IDs win");
        return EmiSyncedRecipeMaps.fromHolders(combined.values());
    }

    public static RecipeMap getFallbackMap() {
        return fallbackMap;
    }

    public static boolean isUsingFallback() {
        return fallbackMap != null && !EmiClient.onServer;
    }

    public static void reset() {
        fallbackMap = null;
        vanillaMap = null;
        lastServerMap = null;
        connection = null;
        ticksWithoutRecipes = 0;
        attempted = false;
        VanillaRecipeTags.clear();
    }
}
