package dev.emi.emi.mixin;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Ordering;

import dev.emi.emi.config.EffectLocation;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.mixin.accessor.HandledScreenAccessor;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.text.Text;

/**
 * Minecraft 1.21.2 moved status-effect rendering out of the old
 * AbstractInventoryScreen and into StatusEffectsDisplay. Keep EMI's configurable
 * effect locations by drawing the same vanilla effect widgets from the new owner.
 */
@Mixin(StatusEffectsDisplay.class)
public abstract class StatusEffectsDisplayMixin {
	@Shadow @Final
	private HandledScreen<?> parent;

	@Shadow @Final
	private MinecraftClient client;

	@Shadow
	private Text getStatusEffectDescription(StatusEffectInstance effect) {
		throw new UnsupportedOperationException();
	}

	@Shadow
	private void drawStatusEffectBackgrounds(DrawContext draw, int x, int height,
			Iterable<StatusEffectInstance> statusEffects, boolean wide) {
		throw new UnsupportedOperationException();
	}

	@Shadow
	private void drawStatusEffectSprites(DrawContext draw, int x, int height,
			Iterable<StatusEffectInstance> statusEffects, boolean wide) {
		throw new UnsupportedOperationException();
	}

	@Shadow
	private void drawStatusEffectDescriptions(DrawContext draw, int x, int height,
			Iterable<StatusEffectInstance> statusEffects) {
		throw new UnsupportedOperationException();
	}

	@Inject(
		method = "drawStatusEffects(Lnet/minecraft/client/gui/DrawContext;IIF)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void emi$drawConfiguredStatusEffects(DrawContext raw, int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
		EffectLocation location = EmiConfig.effectLocation;
		if (location == EffectLocation.HIDDEN) {
			ci.cancel();
			return;
		}

		// RIGHT is vanilla's normal presentation. Let Minecraft handle it so any
		// future tooltip/rendering details remain native when no relocation is needed.
		if (location == EffectLocation.RIGHT) {
			return;
		}

		Collection<StatusEffectInstance> effects = Ordering.natural().sortedCopy(client.player.getStatusEffects());
		if (effects.isEmpty()) {
			ci.cancel();
			return;
		}

		ci.cancel();
		if (location == EffectLocation.TOP) {
			emi$drawTop(raw, mouseX, mouseY, effects);
		} else {
			emi$drawVertical(raw, mouseX, mouseY, effects, location);
		}
	}

	@Unique
	private void emi$drawTop(DrawContext raw, int mouseX, int mouseY, Collection<StatusEffectInstance> effects) {
		HandledScreenAccessor screen = (HandledScreenAccessor) parent;
		int size = effects.size();
		boolean wide = size == 1;
		int targetY = screen.getY() - 34;
		if (parent instanceof CreativeInventoryScreen) {
			targetY -= 28;
			if (EmiAgnos.isForge()) {
				targetY -= 22;
			}
		}

		int xOff = 34;
		if (wide) {
			xOff = 122;
		} else if (size > 5) {
			xOff = (screen.getBackgroundWidth() - 32) / (size - 1);
		}
		int width = (size - 1) * xOff + (wide ? 120 : 32);
		int x = screen.getX() + (screen.getBackgroundWidth() - width) / 2;

		EmiDrawContext context = EmiDrawContext.wrap(raw);
		context.resetColor();
		StatusEffectInstance hovered = null;
		int dy = targetY - screen.getY();
		context.push();
		try {
			context.matrices().translate(0, dy, 0);
			for (StatusEffectInstance effect : effects) {
				int effectWidth = wide ? 120 : 32;
				List<StatusEffectInstance> single = List.of(effect);
				drawStatusEffectBackgrounds(raw, x, 32, single, wide);
				drawStatusEffectSprites(raw, x, 32, single, wide);
				if (wide) {
					drawStatusEffectDescriptions(raw, x, 32, single);
				}
				if (mouseX >= x && mouseX < x + effectWidth && mouseY >= targetY && mouseY < targetY + 32) {
					hovered = effect;
				}
				x += xOff;
			}
		} finally {
			context.pop();
		}

		if (hovered != null && size > 1) {
			emi$drawEffectTooltip(raw, hovered, mouseX, mouseY);
		}
	}

	@Unique
	private void emi$drawVertical(DrawContext raw, int mouseX, int mouseY,
			Collection<StatusEffectInstance> effects, EffectLocation location) {
		HandledScreenAccessor screen = (HandledScreenAccessor) parent;
		boolean wide = !location.compressed;
		int spacing = effects.size() > 5 ? 132 / (effects.size() - 1) : 33;
		int effectWidth = wide ? 120 : 32;
		int x;
		if (location == EffectLocation.LEFT || location == EffectLocation.LEFT_COMPRESSED) {
			x = screen.getX() - 2 - effectWidth;
		} else {
			x = screen.getX() + screen.getBackgroundWidth() + 2;
		}

		drawStatusEffectBackgrounds(raw, x, spacing, effects, wide);
		drawStatusEffectSprites(raw, x, spacing, effects, wide);
		if (wide) {
			drawStatusEffectDescriptions(raw, x, spacing, effects);
		}

		if (!wide) {
			int y = screen.getY();
			for (StatusEffectInstance effect : effects) {
				if (mouseX >= x && mouseX < x + effectWidth && mouseY >= y && mouseY < y + 32) {
					emi$drawEffectTooltip(raw, effect, mouseX, mouseY);
					break;
				}
				y += spacing;
			}
		}
	}

	@Unique
	private void emi$drawEffectTooltip(DrawContext raw, StatusEffectInstance effect, int mouseX, int mouseY) {
		List<Text> tooltip = List.of(
			getStatusEffectDescription(effect),
			StatusEffectUtil.getDurationText(effect, 1.0f, client.world.getTickManager().getTickRate())
		);
		raw.drawTooltip(client.textRenderer, tooltip, Optional.empty(), mouseX, Math.max(mouseY, 16));
	}
}
