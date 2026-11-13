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
import dev.emi.emi.runtime.EmiClientRecipeFallback;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadManager;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Shadow
    public ClientLevel level;

    @Inject(at = @At("RETURN"), method = "reloadResourcePacks", require = 0)
    private void reloadResources(CallbackInfoReturnable<CompletableFuture<Void>> info) {
        CompletableFuture<Void> future = info.getReturnValue();
        if (future != null) {
            // A reload may complete on a worker thread. Schedule the state
            // check on the real game thread; do not create an unmanaged pool.
            future.thenRun(() -> Minecraft.getInstance().execute(() -> {
                Minecraft client = Minecraft.getInstance();
                if (client.level != null && EmiPort.getRecipeMap() != null) {
                    EmiReloadManager.reload();
                }
            }));
        }
    }

    /** Tick the fallback on the game client thread. This is not a server hook. */
    @Inject(at = @At("TAIL"), method = "tick")
    private void pfemi$clientRecipeFallbackTick(CallbackInfo info) {
        EmiClientRecipeFallback.tick();
    }

    @Inject(at = @At("HEAD"), method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V")
    private void disconnect(CallbackInfo info) {
        // Drop old fallback before clearing the EMI recipe model.
        EmiClientRecipeFallback.reset();
        EmiLog.info("Disconnecting from server, EMI data cleared");
        EmiReloadManager.clear();
        EmiClient.onServer = false;
    }
}
