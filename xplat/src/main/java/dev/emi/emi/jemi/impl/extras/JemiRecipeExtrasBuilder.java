package dev.emi.emi.jemi.impl.extras;

import java.util.List;

import com.google.common.collect.Lists;

import dev.emi.emi.api.render.EmiTexture;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.placement.IPlaceable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IDrawableWidget;
import mezz.jei.api.gui.widgets.IButtonWidget;
import mezz.jei.api.gui.widgets.ICycleButtonWidget;
import java.util.function.Consumer;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.gui.widgets.IScrollBoxWidget;
import mezz.jei.api.gui.widgets.IScrollGridWidget;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import mezz.jei.api.gui.widgets.ITextWidget;
import net.minecraft.network.chat.FormattedText;

public class JemiRecipeExtrasBuilder implements IRecipeExtrasBuilder {
	public IRecipeSlotDrawablesView slots;
	public List<IJeiInputHandler> inputHandlers = Lists.newArrayList();
	public List<IJeiGuiEventListener> eventListeners = Lists.newArrayList();
	public List<JemiWidgetBuilder> widgets = Lists.newArrayList();
	public List<JemiScrollGridWidget> scrollGrids = Lists.newArrayList();

	public JemiRecipeExtrasBuilder(IRecipeSlotDrawablesView slots) {
		this.slots = slots;
	}

	@Override
	public IRecipeSlotDrawablesView getRecipeSlots() {
		return slots;
	}

	// Retained for existing JEI plugins; modern widget overloads are above.
	@SuppressWarnings("removal")
	@Override
	public void addDrawable(IDrawable drawable, int xPos, int yPos) {
		addDrawable(drawable).setPosition(xPos, yPos);
	}

	// Retained for existing JEI plugins; modern widget overloads are above.
	@SuppressWarnings("removal")
	@Override
	public IPlaceable<?> addDrawable(IDrawable drawable) {
		return addEmi(new JemiWidgetBuilder(drawable.getWidth(), drawable.getHeight(), (self, holder) -> {
			holder.addDrawable(self.x, self.y, drawable.getWidth(), drawable.getHeight(), (raw, mouseX, mouseY, delta) -> {
				drawable.draw(raw);
			});
		}));
	}

    @Override
    public IDrawableWidget addDrawableWidget(IDrawable drawable) {
        JemiWidgetBuilder widget = addEmi(new JemiWidgetBuilder(drawable.getWidth(), drawable.getHeight(),
            (self, holder) -> holder.addDrawable(self.x, self.y, drawable.getWidth(), drawable.getHeight(),
                (raw, mx, my, delta) -> drawable.draw(raw))));
        return new JemiWidgetAdapters.Drawable(widget);
    }

    @Override
    public IDrawableWidget addTooltipArea(int xPos, int yPos, int width, int height) {
        JemiWidgetBuilder widget = addEmi(new JemiWidgetBuilder(width, height, (self, holder) -> { }));
        widget.setPosition(xPos, yPos);
        return new JemiWidgetAdapters.Drawable(widget);
    }

    @Override
    public IButtonWidget<?> addButton(int width, int height, Runnable onPress) {
        final JemiWidgetAdapters.Button[] adapter = new JemiWidgetAdapters.Button[1];
        JemiWidgetBuilder widget = addEmi(new JemiWidgetBuilder(width, height,
            (self, holder) -> holder.addButton(self.x, self.y, self.width, self.height, 0, 0,
                () -> adapter[0] != null && adapter[0].active && adapter[0].visible,
                (mx, my, btn) -> { if (adapter[0] != null && adapter[0].active && adapter[0].visible) onPress.run(); })));
        adapter[0] = new JemiWidgetAdapters.Button(widget);
        return adapter[0];
    }

    @Override
    public <T> ICycleButtonWidget<T> addCycleButton(List<T> values, T initialValue, int width, int height, Consumer<T> onValueChanged) {
        final int[] index = {Math.max(0, values.indexOf(initialValue))};
        final java.util.concurrent.atomic.AtomicReference<JemiWidgetAdapters.Cycle<T>> state = new java.util.concurrent.atomic.AtomicReference<>();
        JemiWidgetBuilder widget = addEmi(new JemiWidgetBuilder(width, height,
            (self, holder) -> holder.addButton(self.x, self.y, self.width, self.height, 0, 0,
                () -> !values.isEmpty(),
                (mx, my, btn) -> {
                    if (values.isEmpty()) return;
                    index[0] = (index[0]+1) % values.size();
                    T next = values.get(index[0]);
                    state.get().setValue(next);
                    onValueChanged.accept(next);
                })));
        JemiWidgetAdapters.Cycle<T> adapter = new JemiWidgetAdapters.Cycle<>(widget, initialValue);
        state.set(adapter);
        return adapter;
    }

    @Override
    public IDrawableWidget addRecipeArrowWidget() {
        return new JemiWidgetAdapters.Drawable(addEmiTextureAsWidget(EmiTexture.EMPTY_ARROW));
    }

    @Override
    public IDrawableWidget addRecipePlusSignWidget() {
        return new JemiWidgetAdapters.Drawable(addEmiTextureAsWidget(EmiTexture.PLUS));
    }

    @Override
    public IDrawableWidget addAnimatedRecipeArrowWidget(int ticksPerCycle) {
        return new JemiWidgetAdapters.Drawable(addAnimatedTextureAsWidget(EmiTexture.EMPTY_ARROW, EmiTexture.FULL_ARROW, ticksPerCycle*50, true, false, false));
    }

    @Override
    public IDrawableWidget addAnimatedRecipeFlameWidget(int cookTime) {
        return new JemiWidgetAdapters.Drawable(addAnimatedTextureAsWidget(EmiTexture.EMPTY_FLAME, EmiTexture.FULL_FLAME, cookTime*50, false, true, true));
    }

    private JemiWidgetBuilder addEmiTextureAsWidget(EmiTexture texture) {
        return addEmi(new JemiWidgetBuilder(texture.width, texture.height,
            (self, holder) -> holder.addTexture(texture, self.x, self.y)));
    }

    private JemiWidgetBuilder addAnimatedTextureAsWidget(EmiTexture texture, EmiTexture animated, int millis,
            boolean horizontal, boolean endToStart, boolean fullToEmpty) {
        return addEmi(new JemiWidgetBuilder(texture.width, texture.height, (self, holder) -> {
            holder.addTexture(texture, self.x, self.y);
            holder.addAnimatedTexture(animated, self.x, self.y, millis, horizontal, endToStart, fullToEmpty);
        }));
    }

	@Override
	public void addWidget(IRecipeWidget widget) {
		addEmi(new JemiWidgetBuilder(0, 0, (self, holder) -> {
			holder.addDrawable(self.x, self.y, 0, 0, (raw, mouseX, mouseY, delta) -> {
				widget.drawWidget(raw, mouseX, mouseY);
			});
			// Tooltip not implemented as JEI widgets have no bounds
		}));
	}

	@Override
	public void addSlottedWidget(ISlottedRecipeWidget widget, List<IRecipeSlotDrawable> slots) {
		// EMI's understanding of slots doesn't mesh with this
		addWidget(widget);
	}

	@Override
	public void addInputHandler(IJeiInputHandler inputHandler) {
		inputHandlers.add(inputHandler);
	}

	@Override
	public void addGuiEventListener(IJeiGuiEventListener guiEventListener) {
		eventListeners.add(guiEventListener);
	}

	@Override
	public IScrollBoxWidget addScrollBoxWidget(int width, int height, int xPos, int yPos) {
		return new JemiScrollBoxWidget(xPos, yPos, width, height);
	}

	@Override
	public IScrollGridWidget addScrollGridWidget(List<IRecipeSlotDrawable> slots, int columns, int visibleRows) {
		JemiScrollGridWidget grid = new JemiScrollGridWidget(slots, 0, 0, columns, visibleRows);
		scrollGrids.add(grid);
		return grid;
	}

	// Retained for existing JEI plugins; modern widget overloads are above.
	@SuppressWarnings("removal")
	@Override
	public IPlaceable<?> addRecipeArrow() {
		return addEmiTexture(EmiTexture.EMPTY_ARROW);
	}

	// Retained for existing JEI plugins; modern widget overloads are above.
	@SuppressWarnings("removal")
	@Override
	public IPlaceable<?> addRecipePlusSign() {
		return addEmiTexture(EmiTexture.PLUS);
	}

	// Retained for existing JEI plugins; modern widget overloads are above.
	@SuppressWarnings("removal")
	@Override
	public IPlaceable<?> addAnimatedRecipeArrow(int ticksPerCycle) {
		return addAnimatedEmiTexture(EmiTexture.EMPTY_ARROW, EmiTexture.FULL_ARROW, ticksPerCycle * 1000 / 20, true, false, false);
	}

	// Retained for existing JEI plugins; modern widget overloads are above.
	@SuppressWarnings("removal")
	@Override
	public IPlaceable<?> addAnimatedRecipeFlame(int cookTime) {
		return addAnimatedEmiTexture(EmiTexture.EMPTY_FLAME, EmiTexture.FULL_FLAME, cookTime * 1000 / 20, false, true, true);
	}

	@Override
	public ITextWidget addText(List<FormattedText> text, int maxWidth, int maxHeight) {
		return new JemiTextWidget(text, maxWidth, maxHeight);
	}

	private IPlaceable<?> addEmiTexture(EmiTexture texture) {
		return addEmi(new JemiWidgetBuilder(texture.width, texture.height, (self, holder) -> {
			holder.addTexture(texture, self.x, self.y);
		}));
	}

	private IPlaceable<?> addAnimatedEmiTexture(EmiTexture texture, EmiTexture animated, int time, boolean horizontal, boolean endToStart, boolean fullToEmpty) {
		return addEmi(new JemiWidgetBuilder(texture.width, texture.height, (self, holder) -> {
			holder.addTexture(texture, self.x, self.y);
			holder.addAnimatedTexture(animated, self.x, self.y, time, horizontal, endToStart, fullToEmpty);
		}));
	}

	private JemiWidgetBuilder addEmi(JemiWidgetBuilder builder) {
		widgets.add(builder);
		return builder;
	}
}
