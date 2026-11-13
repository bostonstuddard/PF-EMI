package dev.emi.emi.stack;

import java.util.List;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Items;

/** A searchable, non-item result for an effect, including non-brewable effects. */
public final class MobEffectEmiStack extends EmiStack {
    private final MobEffect effect;

    public MobEffectEmiStack(MobEffect effect) {
        this.effect = effect;
    }

    @Override
    public EmiStack copy() {
        MobEffectEmiStack copy = new MobEffectEmiStack(effect);
        copy.amount = amount;
        copy.chance = chance;
        copy.comparison = comparison;
        copy.setRemainder(getRemainder().copy());
        return copy;
    }

    @Override
    public boolean isEmpty() { return false; }

    @Override
    public DataComponentPatch getComponentChanges() { return DataComponentPatch.EMPTY; }

    @Override
    public Object getKey() { return effect; }

    @Override
    public Identifier getId() { return BuiltInRegistries.MOB_EFFECT.getKey(effect); }

    @Override
    public Component getName() { return effect.getDisplayName(); }

    @Override
    public void render(GuiGraphicsExtractor draw, int x, int y, float delta, int flags) {
        if ((flags & RENDER_ICON) != 0) {
            EmiStack.of(Items.POTION).render(draw, x, y, delta, RENDER_ICON);
            int color = 0xff000000 | effect.getColor();
            EmiDrawContext.wrap(draw).fill(x + 11, y + 1, 4, 4, color);
        }
    }

    @Override
    public List<Component> getTooltipText() {
        return List.of(getName(), EmiPort.literal("Click to view status effect details and sources"));
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        return getTooltipText().stream()
            .map(c -> ClientTooltipComponent.create(EmiPort.ordered(c)))
            .toList();
    }
}
