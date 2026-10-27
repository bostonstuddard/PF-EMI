package dev.emi.emi.recipe.special;

import java.util.List;
import java.util.Random;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiPatternCraftingRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.GeneratedSlotWidget;
import dev.emi.emi.api.widget.SlotWidget;

public class EmiBannerDuplicateRecipe extends EmiPatternCraftingRecipe {

	public static final List<Item> BANNERS = List.of(
			dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "white_banner")), dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "orange_banner")), dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "magenta_banner")), dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "light_blue_banner")),
			dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "yellow_banner")), dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "lime_banner")), dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "pink_banner")), dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "gray_banner")),
			dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "light_gray_banner")), dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "cyan_banner")), dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "purple_banner")), dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "blue_banner")),
			dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "brown_banner")), dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "green_banner")), dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "red_banner")), dev.emi.emi.EmiPort.getItemRegistry().getValue(EmiPort.id("minecraft", "black_banner"))
	);

	private final Item banner;

	public EmiBannerDuplicateRecipe(Item banner, Identifier id) {
		super(List.of(
				EmiStack.of(banner),
				EmiStack.of(banner).setRemainder(EmiStack.of(banner))),
				EmiStack.of(banner), id);
		this.banner = banner;
	}

	@Override
	public SlotWidget getInputWidget(int slot, int x, int y) {
		if (slot == 0) {
			return new SlotWidget(EmiStack.of(banner), x, y);
		} else if (slot == 1) {
			return new GeneratedSlotWidget(r -> getPattern(r, true), unique, x, y);
		}
		return new SlotWidget(EmiStack.EMPTY, x, y);
	}

	@Override
	public SlotWidget getOutputWidget(int x, int y) {
		return new GeneratedSlotWidget(r -> getPattern(r, false) , unique, x, y);
	}

	public EmiStack getPattern(Random random, boolean reminder) {
		ItemStack stack = new ItemStack(banner);
		int patterns = 1 + Math.max(random.nextInt(5), random.nextInt(3));
		BannerPatternLayers pattern = BannerPatternLayers.EMPTY;
		for (int i = 0; i < patterns; i++) {
			pattern = EmiPort.addRandomBanner(pattern, random);
		}

		stack.set(DataComponents.BANNER_PATTERNS, pattern);

		EmiStack emiStack = EmiStack.of(stack);
		if (reminder) {
			emiStack.setRemainder(EmiStack.of(stack));
		}
		return emiStack;
	}
}
