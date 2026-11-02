package dev.emi.emi.runtime;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/**
 * Small 1.21.2 compatibility bridge for DrawContext's texture API.
 * 1.21.2 added a render-layer factory argument and removed the direct z overloads.
 */
public final class EmiDrawCompat {
	private EmiDrawCompat() {
	}

	public static void drawTexture(DrawContext context, Identifier texture,
			int x, int y, int u, int v, int width, int height) {
		context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, u, v, width, height, 256, 256);
	}

	public static void drawTexture(DrawContext context, Identifier texture,
			int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, u, v, width, height, textureWidth, textureHeight);
	}

	public static void drawTexture(DrawContext context, Identifier texture,
			int x, int y, int z, int u, int v, int width, int height) {
		context.getMatrices().push();
		context.getMatrices().translate(0, 0, z);
		drawTexture(context, texture, x, y, u, v, width, height);
		context.getMatrices().pop();
	}

	public static void drawTexture(DrawContext context, Identifier texture,
			int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		context.getMatrices().push();
		context.getMatrices().translate(0, 0, z);
		context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, u, v, width, height, textureWidth, textureHeight);
		context.getMatrices().pop();
	}

	public static void drawTexture(DrawContext context, Identifier texture,
			int x, int y, int width, int height, float u, float v,
			int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
		context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, u, v, width, height,
			regionWidth, regionHeight, textureWidth, textureHeight);
	}
	// Most EMI widgets render through EmiDrawContext. Keep the vanilla DrawContext
	// overloads above as the single implementation and unwrap here.
	public static void drawTexture(EmiDrawContext context, Identifier texture,
			int x, int y, int u, int v, int width, int height) {
		drawTexture(context.raw(), texture, x, y, u, v, width, height);
	}

	public static void drawTexture(EmiDrawContext context, Identifier texture,
			int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		drawTexture(context.raw(), texture, x, y, u, v, width, height, textureWidth, textureHeight);
	}

	public static void drawTexture(EmiDrawContext context, Identifier texture,
			int x, int y, int z, int u, int v, int width, int height) {
		drawTexture(context.raw(), texture, x, y, z, u, v, width, height);
	}

	public static void drawTexture(EmiDrawContext context, Identifier texture,
			int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		drawTexture(context.raw(), texture, x, y, z, u, v, width, height, textureWidth, textureHeight);
	}

	public static void drawTexture(EmiDrawContext context, Identifier texture,
			int x, int y, int width, int height, float u, float v,
			int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
		drawTexture(context.raw(), texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
	}
}
