package dev.emi.emi.screen;

import dev.emi.emi.EmiPort;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.util.Identifier;

public class DisabledToast implements Toast {
	private static final Identifier TEXTURE = EmiPort.id("toast/advancement");
	private Visibility visibility = Visibility.SHOW;

	@Override
	public void draw(DrawContext raw, TextRenderer textRenderer, long time) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		context.resetColor();
		raw.drawGuiTexture(RenderLayer::getGuiTextured, TEXTURE, 0, 0, this.getWidth(), this.getHeight());
		context.drawCenteredText(EmiPort.translatable("emi.disabled"), getWidth() / 2, 7);
		context.drawCenteredText(EmiConfig.toggleVisibility.getBindText(), getWidth() / 2, 18);
	}

	@Override
	public void update(ToastManager manager, long time) {
		visibility = (time > 8_000 || EmiConfig.enabled) ? Visibility.HIDE : Visibility.SHOW;
	}

	@Override
	public Visibility getVisibility() {
		return visibility;
	}
}
