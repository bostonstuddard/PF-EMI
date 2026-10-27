package dev.emi.emi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Fabric 26.3 receives authoritative RecipeHolders from the Fabric recipe
 * synchronization callback. Vanilla's update-recipes packet contains recipe
 * display metadata, not a full recipe registry; do not treat it as a second
 * recipe-data update or EMI can rebuild twice while joining a world.
 */
@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
	@Unique
	private int infoMask = 0;

	@Inject(at = @At("RETURN"), method = "handleUpdateTags")
	private void refreshTagBasedData(CallbackInfo info) {
		EmiReloadManager.reloadTags();
	}

	@Inject(at = @At("RETURN"), method = "handleLogin")
	private void onGameJoin(CallbackInfo info) {
		EmiLog.info("Joining server, EMI waiting for data from server...");
	}
}
