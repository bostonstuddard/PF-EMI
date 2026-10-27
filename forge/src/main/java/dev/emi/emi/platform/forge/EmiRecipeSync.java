package dev.emi.emi.platform.forge;

import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadManager;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minecraft 26.3 Forge recipe synchronization. The legacy per-recipe
 * StreamCodec cannot represent every valid recipe (e.g. some tag ingredients).
 * Each entry is independently framed and validated. Fall back to the
 * data-pack MapCodec representation without risking a player disconnect.
 * Missing/invalid entries are logged, not silently converted to fake recipes.
 */
public final class EmiRecipeSync {
    private static final int PER_PACKET = 16;
    private static final int MAX_PACKETS = 4096;
    private static final int MAX_CHUNK_BYTES = 96 * 1024;
    private static final int CHUNK_HEADER_ALLOWANCE = 16;
    private static final int MAX_WARNED_RECIPES = 10;
    private static final int MAX_JSON_CHARS = 32_767;
    private static final int STREAM = 0;
    private static final int DATA_PACK = 1;
    private static final Map<Integer, Chunk> pending = new HashMap<>();
    private static int expected = -1;

    private EmiRecipeSync() {}

    /** Fully encoded wire entry; preserves the exact preflighted bytes. */
    private record EncodedRecipe(RecipeHolder<?> holder, byte[] bytes, int format) {}

    public record Chunk(int index, int total, List<EncodedRecipe> recipes) {
        public Chunk(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), decodeRecipes(buf));
        }

        private static List<EncodedRecipe> decodeRecipes(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            if (count < 0 || count > PER_PACKET) {
                throw new IllegalArgumentException("Invalid EMI recipe chunk size: " + count);
            }
            var recipes = new ArrayList<EncodedRecipe>(count);
            int totalBytes = CHUNK_HEADER_ALLOWANCE;
            for (int n = 0; n < count; n++) {
                int length = buf.readVarInt();
                totalBytes += length + 5;
                if (length < 1 || length > MAX_CHUNK_BYTES || totalBytes > MAX_CHUNK_BYTES
                        || length > buf.readableBytes()) {
                    throw new IllegalArgumentException("Invalid EMI recipe entry length: " + length);
                }
                byte[] bytes = new byte[length];
                buf.readBytes(bytes);
                // A single bad modded recipe must not disconnect the whole client:
                // framing allows us to skip just that entry and keep decoding.
                try {
                    int format = bytes[0] & 0xFF;
                    RecipeHolder<?> recipe = decodeRecipe(bytes, buf.registryAccess());
                    recipes.add(new EncodedRecipe(recipe, bytes, format));
                } catch (RuntimeException ex) {
                    EmiLog.warn("Forge could not decode one synced EMI recipe; "
                            + "skipping the framed entry: " + ex);
                }
            }
            return recipes;
        }

        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(index);
            buf.writeVarInt(total);
            buf.writeVarInt(recipes.size());
            for (EncodedRecipe recipe : recipes) {
                buf.writeVarInt(recipe.bytes().length);
                buf.writeBytes(recipe.bytes());
            }
        }
    }

    private static void writeRecipe(RegistryFriendlyByteBuf buf, RecipeHolder<?> holder, int format) {
        buf.writeByte(format);
        if (format == STREAM) {
            RecipeHolder.STREAM_CODEC.encode(buf, holder);
        } else if (format == DATA_PACK) {
            // 26.3 keeps recipes in a reloadable data-pack registry. The
            // structured codec retains tagged ingredients that stream codecs
            // sometimes reject as "Ingredients can't be empty".
            ResourceKey.streamCodec(Registries.RECIPE).encode(buf, holder.id());
            String json = Recipe.DIRECT_CODEC.encodeStart(
                    RegistryOps.create(JsonOps.INSTANCE, buf.registryAccess()),
                    holder.value()).getOrThrow().toString();
            buf.writeUtf(json, MAX_JSON_CHARS);
        } else {
            throw new IllegalArgumentException("Unknown EMI recipe format " + format);
        }
    }

    private static RecipeHolder<?> decodeRecipe(byte[] bytes, RegistryAccess access) {
        RegistryFriendlyByteBuf payload = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), access);
        try {
            int format = payload.readUnsignedByte();
            RecipeHolder<?> recipe;
            if (format == STREAM) {
                recipe = RecipeHolder.STREAM_CODEC.decode(payload);
            } else if (format == DATA_PACK) {
                ResourceKey<Recipe<?>> id = ResourceKey.streamCodec(Registries.RECIPE).decode(payload);
                String json = payload.readUtf(MAX_JSON_CHARS);
                Recipe<?> value = Recipe.DIRECT_CODEC.parse(
                        RegistryOps.create(JsonOps.INSTANCE, access),
                        JsonParser.parseString(json)).getOrThrow();
                recipe = new RecipeHolder<>(id, value);
            } else {
                throw new IllegalArgumentException("Unrecognized EMI recipe format: " + format);
            }
            if (payload.isReadable()) {
                throw new IllegalArgumentException("Trailing bytes in EMI recipe " + recipe.id());
            }
            return recipe;
        } finally {
            payload.release();
        }
    }

    /** Preflight both wire formats with an actual encode/decode roundtrip. */
    private static EncodedRecipe encodeRecipe(RecipeHolder<?> holder, int format, RegistryAccess access) {
        RegistryFriendlyByteBuf payload = new RegistryFriendlyByteBuf(Unpooled.buffer(256), access);
        try {
            writeRecipe(payload, holder, format);
            if (payload.readableBytes() + CHUNK_HEADER_ALLOWANCE > MAX_CHUNK_BYTES) {
                throw new IllegalArgumentException("Recipe larger than maximum chunk");
            }
            byte[] bytes = new byte[payload.readableBytes()];
            payload.getBytes(payload.readerIndex(), bytes);
            RecipeHolder<?> roundtrip = decodeRecipe(bytes, access);
            if (!roundtrip.id().equals(holder.id())) {
                throw new IllegalArgumentException("Recipe ID changed during roundtrip");
            }
            return new EncodedRecipe(holder, bytes, format);
        } finally {
            payload.release();
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
            EmiLog.info("Forge sending " + total + " EMI recipe-sync chunks to "
                    + player.getName().getString());
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
    private static List<List<EncodedRecipe>> prepareChunks(
            List<RecipeHolder<?>> recipes, RegistryAccess registryAccess) {
        var result = new ArrayList<List<EncodedRecipe>>();
        var batch = new ArrayList<EncodedRecipe>(PER_PACKET);
        int batchBytes = CHUNK_HEADER_ALLOWANCE;
        int omitted = 0;
        int recovered = 0;

        for (RecipeHolder<?> holder : recipes) {
            EncodedRecipe entry = null;
            Exception lastError = null;
            for (int format : new int[] { STREAM, DATA_PACK }) {
                try {
                    entry = encodeRecipe(holder, format, registryAccess);
                    if (format == DATA_PACK) recovered++;
                    break;
                } catch (Exception exception) {
                    lastError = exception;
                }
            }
            if (entry == null) {
                if (omitted < MAX_WARNED_RECIPES) {
                    EmiLog.warn("Forge could not sync recipe " + holder.id()
                            + " using either codec: " + lastError);
                }
                omitted++;
                continue;
            }
            int entryBytes = entry.bytes().length + 5; // length VarInt worst case
            if (!batch.isEmpty() &&
                    (batch.size() >= PER_PACKET || batchBytes + entryBytes > MAX_CHUNK_BYTES)) {
                result.add(List.copyOf(batch));
                batch.clear();
                batchBytes = CHUNK_HEADER_ALLOWANCE;
            }
            batch.add(entry);
            batchBytes += entryBytes;
        }
        if (!batch.isEmpty() || result.isEmpty()) result.add(List.copyOf(batch));
        EmiLog.info("Forge recipe sync: " + (recipes.size() - omitted) + "/" + recipes.size()
                + " recipes encoded; " + recovered + " required 26.3 data-pack codec fallback"
                + (omitted > 0 ? "; " + omitted + " still unsupported" : "; no omissions"));
        if (omitted > 0) {
            EmiLog.warn("Forge recipe sync omitted " + omitted + " of " + recipes.size()
                    + " recipes after trying both codecs. "
                    + (omitted > MAX_WARNED_RECIPES ? "Only first " + MAX_WARNED_RECIPES
                    + " failures logged." : "See individual warnings above."));
        }
        return result;
    }

    private static void accept(Chunk chunk) {
        if (chunk.total <= 0 || chunk.total > MAX_PACKETS || chunk.index < 0 || chunk.index >= chunk.total) return;
        if (chunk.index == 0) {
            EmiLog.info("Forge received first EMI recipe-sync chunk; expected total " + chunk.total);
        }
        if (chunk.index == 0 || expected != chunk.total) {
            pending.clear();
            expected = chunk.total;
            EmiAgnosForge.setReceivedRecipeMap(null);
        }
        pending.put(chunk.index, chunk);
        if (pending.size() != expected) return;
        for (int index = 0; index < expected; index++) {
            if (!pending.containsKey(index)) return;
        }
        // Minecraft 26.3 does not expose the reloadable minecraft:recipe
        // registry through the client connection. Use the recipes from all
        // decoded chunks to construct a separate recipe lookup.
        List<RecipeHolder<?>> allRecipes = new ArrayList<>();
        for (int index = 0; index < expected; index++) {
            for (EncodedRecipe recipe : pending.get(index).recipes()) {
                allRecipes.add(recipe.holder());
            }
        }
        EmiAgnosForge.setReceivedRecipeMap(dev.emi.emi.EmiSyncedRecipeMaps.fromHolders(allRecipes));
        EmiLog.info("Forge received all " + expected + " recipe-sync chunks ("
                + allRecipes.size() + " recipes); initializing EMI");
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
