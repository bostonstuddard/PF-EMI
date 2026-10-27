package dev.emi.emi.mixin;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.emi.EmiPort;
import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadManager;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
	@Shadow
	public ClientLevel level;

	// Minecraft 26.2 changed the implementation signature of the resource reload
	// method. Match it by name and do not make mod loading depend on this
	// optional hook; loader-specific resource listeners also register above.
	@Inject(at = @At("RETURN"), method = "reloadResourcePacks", require = 0)
	private void reloadResources(CallbackInfoReturnable<CompletableFuture<Void>> info) {
		CompletableFuture<Void> future = info.getReturnValue();
		if (future != null) {
			// Return to the Minecraft client thread instead of creating a new
			// non-daemon thread pool every time resource packs are reloaded.
			future.thenRun(() -> Minecraft.getInstance().execute(() -> {
				Minecraft client = Minecraft.getInstance();
				if (client.level != null && EmiPort.getRecipeMap() != null) {
					EmiReloadManager.reload();
				}
			}));
		}
	}

	@Inject(at = @At("HEAD"), method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V")
	private void disconnect(CallbackInfo info) {
		EmiLog.info("Disconnecting from server, EMI data cleared");
		EmiReloadManager.clear();
		EmiClient.onServer = false;
	}
}
