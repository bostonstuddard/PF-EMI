package dev.emi.emi.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.emi.emi.registry.EmiSyncedRecipes;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadManager;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryKey;

/**
 * PF-EMI's full recipe-object synchronization packet.
 *
 * <p>Minecraft no longer synchronizes Recipe instances to clients. PF-EMI still
 * needs the authoritative recipe objects for its index, so this packet sends them
 * with the recipe data codecs instead of the deprecated vanilla recipe packet
 * codecs. The old packet codecs cannot represent empty shaped-grid cells on
 * 1.21.7 and reject many perfectly valid vanilla recipes.</p>
 */
public final class RecipeSyncS2CPacket implements EmiPacket {
	private static final PacketCodec<RegistryByteBuf, RegistryKey<Recipe<?>>> RECIPE_KEY_CODEC =
		PacketCodecs.registryCodec(Recipe.KEY_CODEC);
	private static final PacketCodec<RegistryByteBuf, Recipe<?>> RECIPE_CODEC =
		PacketCodecs.registryCodec(Recipe.CODEC);

	private final List<RecipeEntry<?>> recipes;

	public RecipeSyncS2CPacket(Collection<RecipeEntry<?>> recipes) {
		this.recipes = List.copyOf(recipes);
	}

	public RecipeSyncS2CPacket(RegistryByteBuf buf) {
		int size = buf.readVarInt();
		List<RecipeEntry<?>> recipes = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			RegistryKey<Recipe<?>> id = RECIPE_KEY_CODEC.decode(buf);
			Recipe<?> recipe = RECIPE_CODEC.decode(buf);
			recipes.add(new RecipeEntry<>(id, recipe));
		}
		this.recipes = List.copyOf(recipes);
	}

	@Override
	public void write(RegistryByteBuf buf) {
		// Recipe.PACKET_CODEC and the serializers' packetCodec() methods are obsolete
		// on 1.21.7. In particular, shaped recipes with blank cells can fail with
		// "Ingredients can't be empty" even though the recipe itself is valid.
		// Recipe.CODEC uses the datapack representation, which preserves those blank
		// cells correctly and also dispatches through modded recipe serializers.
		//
		// Encode entries independently so one genuinely broken mod recipe still
		// cannot corrupt the entire custom payload and disconnect the player.
		List<byte[]> encodedRecipes = new ArrayList<>(recipes.size());
		int skipped = 0;

		for (RecipeEntry<?> entry : recipes) {
			RegistryByteBuf scratch = new RegistryByteBuf(Unpooled.buffer(), buf.getRegistryManager());
			try {
				RECIPE_KEY_CODEC.encode(scratch, entry.id());
				RECIPE_CODEC.encode(scratch, entry.value());
				byte[] data = new byte[scratch.readableBytes()];
				scratch.getBytes(scratch.readerIndex(), data);
				encodedRecipes.add(data);
			} catch (RuntimeException e) {
				skipped++;
				if (skipped <= 16) {
					EmiLog.warn("Skipping recipe " + entry.id().getValue()
						+ " because its data codec failed during recipe sync: "
						+ e.getClass().getSimpleName() + ": " + e.getMessage());
				}
			} finally {
				scratch.release();
			}
		}

		if (skipped > 0) {
			String extra = skipped > 16 ? " (only the first 16 were logged)" : "";
			EmiLog.warn("Skipped " + skipped + " recipe(s) whose data codecs failed during PF-EMI sync" + extra);
		}

		buf.writeVarInt(encodedRecipes.size());
		for (byte[] data : encodedRecipes) {
			buf.writeBytes(data);
		}
	}

	@Override
	public void apply(PlayerEntity player) {
		EmiSyncedRecipes.setRecipes(recipes);
		EmiReloadManager.reloadRecipes();
	}

	@Override
	public Id<RecipeSyncS2CPacket> getId() {
		return EmiNetwork.RECIPE_SYNC;
	}
}
