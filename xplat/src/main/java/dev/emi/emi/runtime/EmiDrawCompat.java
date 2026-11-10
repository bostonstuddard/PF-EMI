package dev.emi.emi.runtime;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * DrawContext texture bridge for Minecraft 1.21.6.
 *
 * <p>1.21.6 replaced the render-layer factory parameters with RenderPipeline
 * arguments and removed GUI Z transforms. Positive legacy Z values are mapped
 * to a new root GUI layer.</p>
 */
public final class EmiDrawCompat {
	private EmiDrawCompat() {
	}

	public static void drawTexture(DrawContext context, Identifier texture,
			int x, int y, int u, int v, int width, int height) {
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 256, 256);
	}

	public static void drawTexture(DrawContext context, Identifier texture,
			int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
	}

	public static void drawTexture(DrawContext context, Identifier texture,
			int x, int y, int z, int u, int v, int width, int height) {
		if (z > 0) {
			context.createNewRootLayer();
		}
		drawTexture(context, texture, x, y, u, v, width, height);
	}

	public static void drawTexture(DrawContext context, Identifier texture,
			int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		if (z > 0) {
			context.createNewRootLayer();
		}
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
	}

	public static void drawTexture(DrawContext context, Identifier texture,
			int x, int y, int width, int height, float u, float v,
			int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height,
			regionWidth, regionHeight, textureWidth, textureHeight);
	}

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
