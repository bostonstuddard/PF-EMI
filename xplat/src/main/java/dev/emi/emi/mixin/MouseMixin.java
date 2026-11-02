package dev.emi.emi.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

@Mixin(Mouse.class)
public class MouseMixin {
	@Shadow @Final
	private MinecraftClient client;
	@Shadow
	private double x, y;
	@Shadow
	private int activeButton = -1;

	@Shadow private double cursorDeltaX;

	@Shadow private double cursorDeltaY;

	/*
	 * Minecraft 1.21.2 no longer routes mouse callbacks through
	 * Screen.wrapScreenError. Mouse invokes the Screen methods directly and wraps
	 * failures in CrashReport/CrashException itself, so target those calls instead.
	 */
	@Inject(at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/gui/screen/Screen;mouseClicked(DDI)Z"),
		method = "onMouseButton(JIII)V", cancellable = true)
	private void onMouseDown(long window, int button, int action, int mods, CallbackInfo info) {
		try {
			Screen screen = client.currentScreen;
			if (screen instanceof HandledScreen<?>) {
				double mx = this.x * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
				double my = this.y * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
				if (EmiScreenManager.mouseClicked(mx, my, button)) {
					info.cancel();
				}
			}
		} catch (Exception e) {
			EmiLog.error("Error while handling mouse press", e);
		}
	}

	@Inject(at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/gui/screen/Screen;mouseReleased(DDI)Z"),
		method = "onMouseButton(JIII)V", cancellable = true)
	private void onMouseUp(long window, int button, int action, int mods, CallbackInfo info) {
		try {
			Screen screen = client.currentScreen;
			if (screen instanceof HandledScreen<?>) {
				double mx = this.x * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
				double my = this.y * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
				if (EmiScreenManager.mouseReleased(mx, my, button)) {
					info.cancel();
				}
			}
		} catch (Exception e) {
			EmiLog.error("Error while handling mouse release", e);
		}
	}

	@Inject(at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/gui/screen/Screen;mouseDragged(DDIDD)Z"),
		method = "tick", cancellable = true)
	private void onMouseDragged(CallbackInfo info) {
		try {
			Screen screen = client.currentScreen;
			if (screen instanceof HandledScreen<?>) {
				double mx = this.x * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
				double my = this.y * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
				double dx = this.cursorDeltaX * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
				double dy = this.cursorDeltaY * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
				EmiScreenManager.mouseDragged(mx, my, activeButton, dx, dy);
			}
		} catch (Exception e) {
			EmiLog.error("Error while handling mouse drag", e);
		}
	}

	@Inject(at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/gui/screen/Screen;mouseScrolled(DDDD)Z"),
		method = "onMouseScroll(JDD)V", cancellable = true)
	private void onMouseScrolled(long window, double horizontal, double vertical, CallbackInfo info) {
		try {
			Screen screen = client.currentScreen;
			if (screen instanceof HandledScreen<?>) {
				double amount = (client.options.getDiscreteMouseScroll().getValue() ? Math.signum(vertical) : vertical) * client.options.getMouseWheelSensitivity().getValue();
				double mx = x * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
				double my = y * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
				if (EmiScreenManager.mouseScrolled(mx, my, amount)) {
					info.cancel();
				}
			}
		} catch (Exception e) {
			EmiLog.error("Error while handling mouse scroll", e);
		}
	}
}
