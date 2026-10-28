package dev.emi.emi.jemi.impl;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;

import dev.emi.emi.runtime.EmiLog;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiKeyMapping;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class JemiTooltipBuilder implements ITooltipBuilder {
	public final List<ClientTooltipComponent> tooltip = Lists.newArrayList();
	private final List<Component> legacyText = Lists.newArrayList();
	private final List<Either<FormattedText, TooltipComponent>> lines = Lists.newArrayList();

	@Override
	public void add(FormattedText component) {
		// JEI allows non-text StringVisitable... Minecraft's methods don't easily
		Component text = component instanceof Component nativeText ? nativeText : Component.literal(component.getString());
		tooltip.add(ClientTooltipComponent.create(text.getVisualOrderText()));
		legacyText.add(text);
		lines.add(Either.left(component));
	}

	@Override
	public void addAll(Collection<? extends FormattedText> components) {
		for (FormattedText v : components) {
			add(v);
		}
	}

	@Override
	public void add(TooltipComponent data) {
		try {
			tooltip.add(ClientTooltipComponent.create(data));
			lines.add(Either.right(data));
		} catch (Exception e) {
			EmiLog.error("Error converting TooltipComponent", e);
		}
	}

	@Override
	public void setIngredient(ITypedIngredient<?> typedIngredient) {
		// EMI's methods bypass the vanilla tooltip render which accepts a stack, so this will do nothing
	}

	@Override
	public void clear() {
		// These are only the JEI plugin's lines; EMI's base item tooltip is held separately.
		tooltip.clear();
		legacyText.clear();
		lines.clear();
	}

	@Override
	public void clearIngredient() {
		// EMI does not support tooltip removal
	}

	@Override
	public void addKeyUsageComponent(String translationKey, IJeiKeyMapping keyMapping) {
		// EMI does not use JEI key mappings
	}

	public List<Component> toLegacyToComponents() {
		return legacyText;
	}

	public void removeAll(List<Component> components) {
		// EMI does not support tooltip removal
	}

	@Override
	public List<Either<FormattedText, TooltipComponent>> getLines() {
		return List.copyOf(lines);
	}
}
