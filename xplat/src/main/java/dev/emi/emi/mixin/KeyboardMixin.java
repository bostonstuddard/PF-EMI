package dev.emi.emi.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

@Mixin(Keyboard.class)
public class KeyboardMixin {
	@Shadow @Final
	private MinecraftClient client;

	/*
	 * Minecraft 1.21.2 removed Screen.wrapScreenError from the keyboard input
	 * path and invokes Screen.keyPressed directly inside its own crash wrapper.
	 */
	@Inject(at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/gui/screen/Screen;keyPressed(III)Z"),
		method = "onKey(JIIII)V", cancellable = true)
	public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo info) {
		try {
			Screen screen = client.currentScreen;
			if (screen instanceof HandledScreen<?>) {
				if (action == 1 || action == 2) {
					if (EmiScreenManager.keyPressed(key, scancode, modifiers)) {
						info.cancel();
					}
				}
			}
		} catch (Exception e) {
			EmiLog.error("Error while handling key press", e);
		}
	}

	@Inject(at = @At("HEAD"),
		method = "onChar(JII)V", cancellable = true)
	public void onChar(long window, int codePoint, int modifiers, CallbackInfo info) {
		try {
			if (window == client.getWindow().getHandle()) {
				Screen screen = client.currentScreen;
				if (screen instanceof HandledScreen<?> && this.client.getOverlay() == null) {
					boolean consume = false;
					if (Character.charCount(codePoint) == 1) {
						consume = EmiScreenManager.search.charTyped((char) codePoint, modifiers) || consume;
					} else {
						for (char c : Character.toChars(codePoint)) {
							consume = EmiScreenManager.search.charTyped(c, modifiers) || consume;
						}
					}
					if (consume) {
						info.cancel();
					}
				}
			}
		} catch (Exception e) {
			EmiLog.error("Error while handling char", e);
		}
	}
}
