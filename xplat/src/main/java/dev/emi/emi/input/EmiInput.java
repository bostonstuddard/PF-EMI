package dev.emi.emi.input;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;

public class EmiInput {
	public static final int CONTROL_MASK = 1;
	public static final int ALT_MASK = 2;
	public static final int SHIFT_MASK = 4;

	public static boolean isControlDown() {
		var window = MinecraftClient.getInstance().getWindow();
		if (net.minecraft.util.Util.getOperatingSystem() == net.minecraft.util.Util.OperatingSystem.OSX) {
			return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SUPER)
				|| InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SUPER);
		}
		return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
	}

	public static boolean isAltDown() {
		return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
			|| InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
	}

	public static boolean isShiftDown() {
		return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
			|| InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	public static int maskFromCode(int keyCode) {
		if (net.minecraft.util.Util.getOperatingSystem() == net.minecraft.util.Util.OperatingSystem.OSX) {
			if (keyCode == GLFW.GLFW_KEY_LEFT_SUPER || keyCode == GLFW.GLFW_KEY_RIGHT_SUPER) {
				return CONTROL_MASK;
			}
		}
		if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
			return CONTROL_MASK;
		} else if (keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT) {
			return ALT_MASK;
		} else if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			return SHIFT_MASK;
		}
		return 0;
	}

	public static int getCurrentModifiers() {
		int ret = 0;
		if (isControlDown()) {
			ret |= CONTROL_MASK;
		}
		if (isAltDown()) {
			ret |= ALT_MASK;
		}
		if (isShiftDown()) {
			ret |= SHIFT_MASK;
		}
		return ret;
	}
}
