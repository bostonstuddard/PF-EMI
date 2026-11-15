package dev.emi.emi.runtime;

import org.joml.Matrix3x2fStack;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class EmiDrawContext {
	private final MinecraftClient client = MinecraftClient.getInstance();
	private final DrawContext context;
	private final GuiMatrices matrices;
	
	private EmiDrawContext(DrawContext context) {
		this.context = context;
		this.matrices = new GuiMatrices(context);
	}

	public static EmiDrawContext wrap(DrawContext context) {
		return new EmiDrawContext(context);
	}

	public DrawContext raw() {
		return context;
	}

	/**
	 * Compatibility adapter for EMI's pre-1.21.6 GUI matrix calls.
	 *
	 * <p>Minecraft 1.21.6 changed DrawContext from a 4x4 MatrixStack to a 2D
	 * Matrix3x2fStack. XY transforms still map directly. Positive Z translations
	 * used by EMI for GUI ordering are represented by a new root layer instead.</p>
	 */
	public GuiMatrices matrices() {
		return matrices;
	}

	public void push() {
		matrices.push();
	}

	public void pop() {
		matrices.pop();
	}

	public void newLayer() {
		context.createNewRootLayer();
	}

	public void drawTexture(Identifier texture, int x, int y, int u, int v, int width, int height) {
		drawTexture(texture, x, y, width, height, u, v, width, height, 256, 256);
	}

	public void drawTexture(Identifier texture, int x, int y, int z, float u, float v, int width, int height) {
		drawTexture(texture, x, y, z, u, v, width, height, 256, 256);
	}

	public void drawTexture(Identifier texture, int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		EmiPort.setPositionTexShader();
		dev.emi.emi.runtime.EmiDrawCompat.drawTexture(context, texture, x, y, z, u, v, width, height, textureWidth, textureHeight);
	}

	public void drawTexture(Identifier texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
		EmiPort.setPositionTexShader();
		dev.emi.emi.runtime.EmiDrawCompat.drawTexture(context, texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
	}

	public void fill(int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + height, color);
	}

	private static int opaqueTextColor(int color) {
		return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
	}

	public void drawText(Text text, int x, int y) {
		drawText(text, x, y, -1);
	}

	public void drawText(Text text, int x, int y, int color) {
		context.drawText(client.textRenderer, text, x, y, opaqueTextColor(color), false);
	}

	public void drawText(OrderedText text, int x, int y, int color) {
		context.drawText(client.textRenderer, text, x, y, opaqueTextColor(color), false);
	}

	public void drawTextWithShadow(Text text, int x, int y) {
		drawTextWithShadow(text, x, y, -1);
	}

	public void drawTextWithShadow(Text text, int x, int y, int color) {
		context.drawText(client.textRenderer, text, x, y, opaqueTextColor(color), true);
	}

	public void drawTextWithShadow(OrderedText text, int x, int y, int color) {
		context.drawText(client.textRenderer, text, x, y, opaqueTextColor(color), true);
	}

	public void drawCenteredText(Text text, int x, int y) {
		drawCenteredText(text, x, y, -1);
	}

	public void drawCenteredText(Text text, int x, int y, int color) {
		context.drawText(client.textRenderer, text, x - client.textRenderer.getWidth(text) / 2, y, opaqueTextColor(color), false);
	}

	public void drawCenteredTextWithShadow(Text text, int x, int y) {
		drawCenteredTextWithShadow(text, x, y, -1);
	}

	public void drawCenteredTextWithShadow(Text text, int x, int y, int color) {
		context.drawCenteredTextWithShadow(client.textRenderer, text.asOrderedText(), x, y, opaqueTextColor(color));
	}

	public void enableDepthTest() {
		// Render state is owned by RenderPipeline/GuiRenderState in 1.21.6.
	}

	public void disableDepthTest() {
		// Render state is owned by RenderPipeline/GuiRenderState in 1.21.6.
	}

	public void enableBlend() {
		// Render state is owned by RenderPipeline/GuiRenderState in 1.21.6.
	}

	public void disableBlend() {
		// Render state is owned by RenderPipeline/GuiRenderState in 1.21.6.
	}

	public void resetColor() {
		// Shader color mutation is not part of the deferred 1.21.6 GUI pipeline.
	}

	public void setColor(float r, float g, float b) {
		// Shader color mutation is not part of the deferred 1.21.6 GUI pipeline.
	}

	public void setColor(float r, float g, float b, float a) {
		// Shader color mutation is not part of the deferred 1.21.6 GUI pipeline.
	}

	public void drawStack(EmiIngredient stack, int x, int y) {
		stack.render(raw(), x, y, client.getRenderTickCounter().getTickProgress(false));
	}

	public void drawStack(EmiIngredient stack, int x, int y, int flags) {
		drawStack(stack, x, y, client.getRenderTickCounter().getTickProgress(false), flags);
	}

	public void drawStack(EmiIngredient stack, int x, int y, float delta, int flags) {
		stack.render(raw(), x, y, delta, flags);
	}

	public static final class GuiMatrices {
		private final DrawContext context;

		private GuiMatrices(DrawContext context) {
			this.context = context;
		}

		public Matrix3x2fStack raw() {
			return context.getMatrices();
		}

		public void push() {
			raw().pushMatrix();
		}

		public void pop() {
			raw().popMatrix();
		}

		public void translate(double x, double y, double z) {
			raw().translate((float) x, (float) y);
			if (z > 0) {
				context.createNewRootLayer();
			}
		}

		public void scale(float x, float y, float z) {
			raw().scale(x, y);
		}

		public void scale(double x, double y, double z) {
			raw().scale((float) x, (float) y);
		}

		public void rotate(float radians) {
			raw().rotate(radians);
		}
	}
}
