package dev.emi.emi.api.stack;

import java.util.List;

import org.jetbrains.annotations.ApiStatus;

import com.google.common.collect.Lists;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.render.EmiRender;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiTagKey;
import dev.emi.emi.screen.tooltip.EmiTextTooltipWrapper;
import dev.emi.emi.screen.tooltip.RemainderTooltipComponent;
import dev.emi.emi.screen.tooltip.TagTooltipComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@ApiStatus.Internal
public class TagEmiIngredient implements EmiIngredient {
	private final Identifier id;
	private List<EmiStack> stacks;
	public final TagKey<?> key;
	private final EmiTagKey<?> tagKey;
	private long amount;
	private float chance = 1;

	@ApiStatus.Internal
	public TagEmiIngredient(TagKey<?> key, long amount) {
		this(EmiTagKey.of(key), amount);
	}

	@ApiStatus.Internal
	public TagEmiIngredient(TagKey<?> key, List<EmiStack> stacks, long amount) {
		this(EmiTagKey.of(key), stacks, amount);
	}

	@ApiStatus.Internal
	public TagEmiIngredient(EmiTagKey<?> key, long amount) {
		this(key, EmiTags.getValues(key), amount);
	}

	private TagEmiIngredient(EmiTagKey<?> key, List<EmiStack> stacks, long amount) {
		this.id = key.id();
		this.key = key.raw();
		this.tagKey = key;
		this.stacks = stacks;
		this.amount = amount;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TagEmiIngredient tag && tag.key.equals(this.key);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public EmiIngredient copy() {
		EmiIngredient stack = new TagEmiIngredient(tagKey, amount);
		stack.setChance(chance);
		return stack;
	}

	@Override
	public List<EmiStack> getEmiStacks() {
		return stacks;
	}

	@Override
	public long getAmount() {
		return amount;
	}

	@Override
	public EmiIngredient setAmount(long amount) {
		this.amount = amount;
		return this;
	}

	@Override
	public float getChance() {
		return chance;
	}

	@Override
	public EmiIngredient setChance(float chance) {
		this.chance = chance;
		return this;
	}

	@Override
	public void render(DrawContext draw, int x, int y, float delta, int flags) {
		EmiDrawContext context = EmiDrawContext.wrap(draw);
		MinecraftClient client = MinecraftClient.getInstance();

		if ((flags & RENDER_ICON) != 0 && !stacks.isEmpty()) {
			// The 1.21.2 item renderer no longer exposes the old custom tag-model
			// vertex-consumer path. Render only the representative stack's icon here;
			// amount/tag/remainder overlays belong to this TagEmiIngredient below.
			stacks.get(0).render(context.raw(), x, y, delta, RENDER_ICON);
		}

		if ((flags & RENDER_AMOUNT) != 0 && !tagKey.isOf(EmiPort.getFluidRegistry())) {
			String count = "";
			if (amount != 1) {
				count += amount;
			}
			EmiRenderHelper.renderAmount(context, x, y, EmiPort.literal(count));
		}
		if ((flags & RENDER_INGREDIENT) != 0) {
			EmiRender.renderTagIcon(this, context.raw(), x, y);
		}
		if ((flags & RENDER_REMAINDER) != 0) {
			EmiRender.renderRemainderIcon(this, context.raw(), x, y);
		}
	}

	@Override
	public List<TooltipComponent> getTooltip() {
		List<TooltipComponent> list = Lists.newArrayList();
		list.add(new EmiTextTooltipWrapper(this, EmiPort.ordered(tagKey.getTagName())));
		if (EmiUtil.showAdvancedTooltips()) {
			list.add(TooltipComponent.of(EmiPort.ordered(EmiPort.literal("#" + id, Formatting.DARK_GRAY))));
		}
		if (tagKey.isOf(EmiPort.getFluidRegistry()) && amount > 1) {
			list.add(TooltipComponent.of(EmiPort.ordered(EmiRenderHelper.getAmountText(this, amount))));
		}
		if (EmiConfig.appendModId) {
			String mod = EmiUtil.getModName(id.getNamespace());
			list.add(TooltipComponent.of(EmiPort.ordered(EmiPort.literal(mod, Formatting.BLUE, Formatting.ITALIC))));
		}
		list.add(new TagTooltipComponent(stacks));
		for (EmiStack stack : stacks) {
			if (!stack.getRemainder().isEmpty()) {
				list.add(new RemainderTooltipComponent(this));
				break;
			}
		}
		return list;
	}
}