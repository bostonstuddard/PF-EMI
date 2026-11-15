package dev.emi.emi.screen.tooltip;

import dev.emi.emi.EmiPort;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.text.Text;

public interface EmiTooltipComponent extends TooltipComponent {

	default void drawTooltip(EmiDrawContext context, TooltipRenderData tooltip) {
	}

	default void drawTooltipText(TextRenderData text) {
	}

	int getHeight();

	@Override
	default int getHeight(TextRenderer textRenderer) {
		return getHeight();
	}

	@Override
	default void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext raw) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		context.push();
		context.matrices().translate(x, y, 0);
		MinecraftClient client = MinecraftClient.getInstance();
		drawTooltip(context, new TooltipRenderData(textRenderer, client.getItemRenderer(), x, y));
		context.pop();
	}

	@Override
	default void drawText(DrawContext context, TextRenderer textRenderer, int x, int y) {
		drawTooltipText(new TextRenderData(context, textRenderer, x, y));
	}

	public static class TextRenderData {
		private final DrawContext context;
		public final TextRenderer renderer;
		public final int x, y;
		
		public TextRenderData(DrawContext context, TextRenderer renderer, int x, int y) {
			this.context = context;
			this.renderer = renderer;
			this.x = x;
			this.y = y;
		}

		private static int opaque(int color) {
			return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
		}

		public void draw(String text, int x, int y, int color, boolean shadow) {
			draw(EmiPort.literal(text), x, y, color, shadow);
		}

		public void draw(Text text, int x, int y, int color, boolean shadow) {
			context.drawText(renderer, text, x + this.x, y + this.y, opaque(color), shadow);
		}
	}

	public static class TooltipRenderData {
		public final TextRenderer text;
		public final ItemRenderer item;
		public final int x, y;

		public TooltipRenderData(TextRenderer text, ItemRenderer item, int x, int y) {
			this.text = text;
			this.item = item;
			this.x = x;
			this.y = y;
		}
	}
}
