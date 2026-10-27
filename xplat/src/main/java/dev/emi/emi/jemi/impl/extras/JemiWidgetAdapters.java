package dev.emi.emi.jemi.impl.extras;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Consumer;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IDrawableWidget;
import mezz.jei.api.gui.widgets.IButtonWidget;
import mezz.jei.api.gui.widgets.ICycleButtonWidget;
import mezz.jei.api.gui.widgets.IRecipeWidgetBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidgetTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Adapts JEI 30 widget builder methods to EMI's recipe widget holder. */
public final class JemiWidgetAdapters {
    private JemiWidgetAdapters() { }

    public abstract static class Widget<T extends IRecipeWidgetBuilder<T>> implements IRecipeWidgetBuilder<T> {
        protected final JemiWidgetBuilder widget;
        protected Widget(JemiWidgetBuilder widget) { this.widget = widget; }
        @SuppressWarnings("unchecked") protected T self() { return (T) this; }
        @Override public T setPosition(int x, int y) { widget.setPosition(x, y); return self(); }
        @Override public T setPosition(int x, int y, int w, int h, HorizontalAlignment ha, VerticalAlignment va) {
            return setPosition(x + ha.getXPos(w, widget.width), y + va.getYPos(h, widget.height));
        }
        @Override public int getWidth() { return widget.width; }
        @Override public int getHeight() { return widget.height; }
        @Override public T setTooltip(FormattedText tip) { return self(); }
        @Override public T setTooltip(Collection<? extends FormattedText> tip) { return self(); }
        @Override public T setTooltip(TooltipComponent tip) { return self(); }
        @Override public T setTooltip(IRecipeWidgetTooltipCallback tip) { return self(); }
    }

    public static final class Drawable extends Widget<IDrawableWidget> implements IDrawableWidget {
        public Drawable(JemiWidgetBuilder widget) { super(widget); }
    }

    public static class Button extends Widget<Button> implements IButtonWidget<Button> {
        protected boolean active = true;
        protected boolean visible = true;
        protected Component message = Component.empty();
        protected IDrawable icon;
        public Button(JemiWidgetBuilder widget) { super(widget); }
        @Override public Button setMessage(Component message) { this.message = message; return this; }
        @Override public Button setIcon(IDrawable icon) { this.icon = icon; return this; }
        @Override public Button setActive(boolean active) { this.active = active; return this; }
        @Override public Button setVisible(boolean visible) { this.visible = visible; return this; }
    }

    public static final class Cycle<T> extends Widget<ICycleButtonWidget<T>> implements ICycleButtonWidget<T> {
        private T value;
        private Function<T, Component> label;
        private Function<T, IDrawable> iconProvider;
        private boolean active = true, visible = true;
        public Cycle(JemiWidgetBuilder widget, T initialValue) { super(widget); value = initialValue; }
        @Override public ICycleButtonWidget<T> setMessage(Component message) { return this; }
        @Override public ICycleButtonWidget<T> setIcon(IDrawable icon) { return this; }
        @Override public ICycleButtonWidget<T> setActive(boolean active) { this.active = active; return this; }
        @Override public ICycleButtonWidget<T> setVisible(boolean visible) { this.visible = visible; return this; }
        @Override public ICycleButtonWidget<T> setMessage(Function<T,Component> label) { this.label = label; return this; }
        @Override public ICycleButtonWidget<T> setIcon(Function<T,IDrawable> icon) { this.iconProvider = icon; return this; }
        @Override public T getValue() { return value; }
        @Override public ICycleButtonWidget<T> setValue(T value) { this.value = value; return this; }
    }
}
