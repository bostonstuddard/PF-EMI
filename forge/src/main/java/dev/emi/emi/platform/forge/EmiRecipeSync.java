package dev.emi.emi.platform.forge;

import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Forge 26.1.2 recipe synchronization for EMI.
 *
 * Vanilla 26.1 recipe serializers are not all safe to stream. Notably,
 * StreamCodec.unit(...) rejects values that are not the serializer's exact
 * canonical instance. An unsendable recipe must never break world join for
 * the whole client. Validate recipes using a registry-aware buffer *before*
 * sending the custom payload, omit unsupported entries with an explicit log
 * message, and keep every chunk comfortably under the payload size cap.
 */
public final class EmiRecipeSync {
    private static final int PER_PACKET = 16;
    private static final int MAX_PACKETS = 4096;
    private static final int MAX_CHUNK_BYTES = 96 * 1024;
    private static final int CHUNK_HEADER_ALLOWANCE = 16;
    private static final int MAX_WARNED_RECIPES = 10;
    private static final Map<Integer, Chunk> pending = new HashMap<>();
    private static int expected = -1;

    private EmiRecipeSync() {}

    public record Chunk(int index, int total, List<RecipeHolder<?>> recipes) {
        public Chunk(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), decodeRecipes(buf));
        }

        private static List<RecipeHolder<?>> decodeRecipes(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            if (size < 0 || size > PER_PACKET) {
                throw new IllegalArgumentException("Invalid EMI recipe chunk size: " + size);
            }
            var recipes = new ArrayList<RecipeHolder<?>>(size);
            for (int n = 0; n < size; n++) {
                recipes.add(RecipeHolder.STREAM_CODEC.decode(buf));
            }
            return recipes;
        }

        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(index);
            buf.writeVarInt(total);
            buf.writeVarInt(recipes.size());
            for (var holder : recipes) {
                try {
                    RecipeHolder.STREAM_CODEC.encode(buf, holder);
                } catch (RuntimeException exception) {
                    // If a registry differs between preflight and network encoding,
                    // the server log must identify the exact recipe that failed.
                    throw new IllegalArgumentException(
                            "PF-EMI Forge failed serializing recipe " + holder.id(), exception);
                }
            }
        }
    }

    public static void register(SimpleChannel channel, int discriminator) {
        channel.messageBuilder(Chunk.class, discriminator, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Chunk::write).decoder(Chunk::new)
                .consumerMainThread((chunk, context) -> {
                    if (context.isClientSide()) accept(chunk);
                    context.setPacketHandled(true);
                }).add();
    }

    public static void sendToPlayers(OnDatapackSyncEvent event) {
        var players = event.getPlayers();
        if (players.isEmpty()) return;
        var recipes = List.copyOf(event.getPlayerList().getServer().getRecipeManager().getRecipes());
        for (ServerPlayer player : players) {
            if (!EmiPacketHandler.CHANNEL.isRemotePresent(player.connection.getConnection())) continue;

            var chunks = prepareChunks(recipes, player.registryAccess());
            if (chunks.size() > MAX_PACKETS) {
                EmiLog.warn("Forge recipe sync requires " + chunks.size()
                        + " packets, exceeding safety limit " + MAX_PACKETS
                        + "; skipping custom recipe sync for " + player.getName().getString());
                continue;
            }
            int total = chunks.size();
            for (int index = 0; index < total; index++) {
                EmiPacketHandler.CHANNEL.send(
                        new Chunk(index, total, chunks.get(index)),
                        PacketDistributor.PLAYER.with(player));
            }
        }
    }

    /**
     * Encode each recipe in isolation using the same stream codec as the
     * outgoing Forge packet. This catches bad serializers before Netty encodes
     * a packet, when an exception would otherwise disconnect the player.
     */
    private static List<List<RecipeHolder<?>>> prepareChunks(
            List<RecipeHolder<?>> recipes, RegistryAccess registryAccess) {
        var result = new ArrayList<List<RecipeHolder<?>>>();
        var batch = new ArrayList<RecipeHolder<?>>(PER_PACKET);
        int batchBytes = CHUNK_HEADER_ALLOWANCE;
        int omitted = 0;

        for (var holder : recipes) {
            int bytes;
            RegistryFriendlyByteBuf probe = new RegistryFriendlyByteBuf(Unpooled.buffer(256), registryAccess);
            try {
                RecipeHolder.STREAM_CODEC.encode(probe, holder);
                bytes = probe.readableBytes();
            } catch (Exception exception) {
                if (omitted < MAX_WARNED_RECIPES) {
                    EmiLog.warn("Forge skipped recipe " + holder.id()
                            + " because its network codec cannot encode it: " + exception);
                }
                omitted++;
                continue;
            } finally {
                probe.release();
            }

            if (bytes > MAX_CHUNK_BYTES - CHUNK_HEADER_ALLOWANCE) {
                if (omitted < MAX_WARNED_RECIPES) {
                    EmiLog.warn("Forge skipped oversized recipe " + holder.id()
                            + " (" + bytes + " encoded bytes)");
                }
                omitted++;
                continue;
            }
            if (!batch.isEmpty() &&
                    (batch.size() >= PER_PACKET || batchBytes + bytes > MAX_CHUNK_BYTES)) {
                result.add(List.copyOf(batch));
                batch.clear();
                batchBytes = CHUNK_HEADER_ALLOWANCE;
            }
            batch.add(holder);
            batchBytes += bytes;
        }
        if (!batch.isEmpty() || result.isEmpty()) result.add(List.copyOf(batch));
        if (omitted > 0) {
            EmiLog.warn("Forge recipe sync omitted " + omitted + " of " + recipes.size()
                    + " recipes that cannot be network encoded. "
                    + "The client will receive all other recipes instead of being disconnected."
                    + (omitted > MAX_WARNED_RECIPES ? " (Only first " + MAX_WARNED_RECIPES
                    + " skipped recipe IDs logged.)" : ""));
        }
        return result;
    }

    private static void accept(Chunk chunk) {
        if (chunk.total <= 0 || chunk.total > MAX_PACKETS || chunk.index < 0 || chunk.index >= chunk.total) return;
        if (chunk.index == 0 || expected != chunk.total) {
            pending.clear();
            expected = chunk.total;
            EmiAgnosForge.setReceivedRecipeMap(null);
        }
        pending.put(chunk.index, chunk);
        if (pending.size() != expected) return;
        var all = new ArrayList<RecipeHolder<?>>();
        for (int index = 0; index < expected; index++) {
            var part = pending.get(index);
            if (part == null) return;
            all.addAll(part.recipes);
        }
        EmiAgnosForge.setReceivedRecipeMap(RecipeMap.create(all));
        pending.clear();
        expected = -1;
        EmiReloadManager.reloadRecipes();
        EmiReloadManager.reloadTags();
    }

    public static void reset() {
        pending.clear();
        expected = -1;
        EmiAgnosForge.setReceivedRecipeMap(null);
    }
}
