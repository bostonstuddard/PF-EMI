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
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

@Mixin(Keyboard.class)
public class KeyboardMixin {
	@Shadow @Final
	private MinecraftClient client;

	/*
	 * Minecraft 1.21.2 removed Screen.wrapScreenError from the keyboard input
	 * path and invokes Screen.keyPressed directly inside its own crash wrapper.
	 */
	@Inject(at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/gui/screen/Screen;keyPressed(Lnet/minecraft/client/input/KeyInput;)Z"),
		method = "onKey(JILnet/minecraft/client/input/KeyInput;)V", cancellable = true)
	public void onKey(long window, int action, KeyInput input, CallbackInfo info) {
		int key = input.key();
		int scancode = input.scancode();
		int modifiers = input.modifiers();
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
		method = "onChar(JLnet/minecraft/client/input/CharInput;)V", cancellable = true)
	public void onChar(long window, CharInput input, CallbackInfo info) {
		int codePoint = input.codepoint();
		int modifiers = input.modifiers();
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
