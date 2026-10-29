package dev.emi.emi.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.emi.emi.registry.EmiSyncedRecipes;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.recipe.RecipeEntry;

/**
 * PF-EMI's 1.21.2 recipe-object synchronization packet.
 *
 * <p>Minecraft 1.21.2 changed the client recipe manager to display-oriented data.
 * EMI's recipe index needs the actual recipe instances, so a PF-EMI server sends
 * the authoritative entries after login.</p>
 */
public final class RecipeSyncS2CPacket implements EmiPacket {
	private final List<RecipeEntry<?>> recipes;

	public RecipeSyncS2CPacket(Collection<RecipeEntry<?>> recipes) {
		this.recipes = List.copyOf(recipes);
	}

	public RecipeSyncS2CPacket(RegistryByteBuf buf) {
		int size = buf.readVarInt();
		List<RecipeEntry<?>> recipes = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			recipes.add(RecipeEntry.PACKET_CODEC.decode(buf));
		}
		this.recipes = List.copyOf(recipes);
	}

	@Override
	public void write(RegistryByteBuf buf) {
		buf.writeVarInt(recipes.size());
		for (RecipeEntry<?> recipe : recipes) {
			RecipeEntry.PACKET_CODEC.encode(buf, recipe);
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
