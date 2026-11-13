package dev.emi.emi.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Shadow @Final private Minecraft minecraft;

    // Ensure the standalone EMI recipe browser receives navigation and
    // use/recipe keys through the same 26.3 input API as inventory screens.
    @Inject(at = @At("HEAD"),
            method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
            cancellable = true, require = 0)
    private void pfemi$recipeScreenKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        try {
            if (window != minecraft.getWindow().handle()) return;
            if (minecraft.gui.screen() instanceof RecipeScreen screen && (action == 1 || action == 2)) {
                if (screen.keyPressed(event)) ci.cancel();
            }
        } catch (Exception e) {
            EmiLog.error("Error handling 26.3 recipe-screen key event", e);
        }
    }

    @Inject(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z",
            ordinal = 0),
            method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", cancellable = true)
    public void onKey(long window, int action, KeyEvent event, CallbackInfo info) {
        try {
            Screen screen = minecraft.gui.screen();
            if (screen instanceof AbstractContainerScreen<?> hs) {
                if (action == 1 || action == 2) {
                    if (EmiScreenManager.keyPressed(event)) info.cancel();
                }
            }
        } catch (Exception e) {
            EmiLog.error("Error while handling key press", e);
        }
    }

    @Inject(at = @At("HEAD"), method = "charTyped(JLnet/minecraft/client/input/CharacterEvent;)V", cancellable = true)
    public void onChar(long window, CharacterEvent event, CallbackInfo info) {
        try {
            if (window == minecraft.getWindow().handle()) {
                Screen screen = minecraft.gui.screen();
                if (screen instanceof AbstractContainerScreen<?> hs && this.minecraft.gui.overlay() == null) {
                    boolean consume = false;
                    consume = EmiScreenManager.search.charTyped(event) || consume;
                    if (consume) info.cancel();
                }
            }
        } catch (Exception e) {
            EmiLog.error("Error while handling char", e);
        }
    }
}
