package dev.emi.emi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;

/**
 * Client-side reload hooks that remain valid on Minecraft 1.21.4.
 *
 * <p>Recipe synchronization is intentionally not injected here. PF-EMI 1.21.4
 * sends complete recipe objects through RecipeSyncS2CPacket, whose apply method
 * updates EmiSyncedRecipes and triggers the recipe reload. The vanilla 1.21.2
 * recipe packet only populates ClientRecipeManager display data, so the old
 * RecipeManager#setRecipes injection is both obsolete and absent at runtime.</p>
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

	@Inject(at = @At("RETURN"), method = "onSynchronizeTags")
	private void refreshTagBasedData(CallbackInfo info) {
		EmiReloadManager.reloadTags();
	}

	@Inject(at = @At("RETURN"), method = "onGameJoin")
	private void onGameJoin(CallbackInfo info) {
		EmiLog.info("Joining server, EMI waiting for data from server...");
	}
}
