package dev.emi.emi.screen;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeDecorator;
import dev.emi.emi.api.widget.RecipeFillButtonWidget;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.TextWidget.Alignment;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.registry.EmiRecipeFiller;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.widget.RecipeDefaultButtonWidget;
import dev.emi.emi.widget.RecipeScreenshotButtonWidget;
import dev.emi.emi.widget.RecipeTreeButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class RecipeDisplay {
	public static final int DISPLAY_PADDING = 8;
	public static final String PF_EMI_UI_PATCH = "PF_EMI_INLINE_RECIPE_BUTTONS_pf.2";

	private static final int ACTION_BUTTON_SIZE = 12;
	private static final int ACTION_BUTTON_STEP = 14;
	private static final int ACTION_ROW_HEIGHT = 17;

	public final EmiRecipe recipe;
	private final int width;
	private final int contentHeight;
	private final List<ButtonType> actionButtons = new ArrayList<>();
	private final int actionRowHeight;
	public Throwable exception;

	public RecipeDisplay(EmiRecipe recipe) {
		this.recipe = recipe;
		this.width = recipe.getDisplayWidth();
		this.contentHeight = recipe.getDisplayHeight();

		// Put every recipe action into one contained row instead of drawing controls
		// outside the recipe card. This fixes PF-EMI controls hanging off the right edge.
		if (EmiConfig.recipeScreenshotButton) {
			actionButtons.add(ButtonType.SCREENSHOT);
		}
		if (EmiRecipeFiller.isSupported(recipe) && EmiConfig.recipeFillButton) {
			actionButtons.add(ButtonType.FILL);
		}
		if (recipe.supportsRecipeTree()) {
			if (EmiConfig.recipeTreeButton) {
				actionButtons.add(ButtonType.TREE);
			}
			if (EmiConfig.recipeDefaultButton) {
				actionButtons.add(ButtonType.DEFAULT);
			}
		}
		this.actionRowHeight = actionButtons.isEmpty() ? 0 : ACTION_ROW_HEIGHT;
	}

	public RecipeDisplay(Throwable exception) {
		this.recipe = null;
		this.width = 128;
		this.contentHeight = 64;
		this.actionRowHeight = 0;
		this.exception = exception;
	}

	public WidgetGroup getWidgets(int x, int y, int availableWidth, int availableHeight) {
		int requestedWidth = getWidth();
		int groupWidth = Math.max(1, Math.min(requestedWidth, availableWidth));
		int groupHeight = Math.max(1, Math.min(getHeight(), availableHeight));
		int wx = x + Math.max(0, (availableWidth - groupWidth) / 2);
		int wy = y;

		WidgetGroup widgets = new WidgetGroup(recipe, wx, wy, groupWidth, groupHeight);
		if (recipe != null) {
			try {
				recipe.addWidgets(widgets);
				if (EmiConfig.showRecipeDecorators) {
					for (EmiRecipeDecorator decorator : EmiRecipes.decorators) {
						decorator.decorateRecipe(recipe, widgets);
					}
				}
				if (EmiConfig.devMode) {
					widgets.decorateDevMode();
				}
				addContainedActionButtons(widgets, groupWidth, groupHeight);
			} catch (Throwable t) {
				EmiLog.error("Error constructing recipe widgets", t);
				widgets = new WidgetGroup(recipe, wx, wy, groupWidth, groupHeight);
				widgets.add(new TextWidget(
					EmiPort.ordered(EmiPort.translatable("emi.error.recipe.render")),
					groupWidth / 2,
					groupHeight / 2 - 5,
					Formatting.RED.getColorValue(),
					true
				).horizontalAlign(Alignment.CENTER));
				if (exception != null) {
					List<Text> text = EmiUtil.getStackTrace(exception).stream()
						.map(s -> (Text) EmiPort.literal(s)).toList();
					widgets.addTooltipText(text, 0, 0, groupWidth, groupHeight);
				}
			}
		} else {
			widgets.add(new TextWidget(
				EmiPort.ordered(EmiPort.translatable("emi.error.recipe.initialize")),
				groupWidth / 2,
				groupHeight / 2 - 5,
				Formatting.RED.getColorValue(),
				true
			).horizontalAlign(Alignment.CENTER));
			if (exception != null) {
				List<Text> text = EmiUtil.getStackTrace(exception).stream()
					.map(s -> (Text) EmiPort.literal(s)).toList();
				widgets.addTooltipText(text, 0, 0, groupWidth, groupHeight);
			}
		}
		return widgets;
	}

	private void addContainedActionButtons(WidgetGroup widgets, int groupWidth, int groupHeight) {
		if (actionButtons.isEmpty() || groupHeight <= contentHeight) {
			return;
		}

		int visibleCount = Math.min(actionButtons.size(), Math.max(0, groupWidth / ACTION_BUTTON_STEP));
		if (visibleCount <= 0) {
			return;
		}

		int totalWidth = visibleCount * ACTION_BUTTON_STEP - 2;
		int x = Math.max(0, (groupWidth - totalWidth) / 2);
		int y = Math.min(groupHeight - ACTION_BUTTON_SIZE, contentHeight + 2);

		for (int i = 0; i < visibleCount; i++) {
			ButtonType type = actionButtons.get(i);
			int bx = x + i * ACTION_BUTTON_STEP;
			widgets.add(switch (type) {
				case FILL -> new RecipeFillButtonWidget(bx, y, recipe);
				case TREE -> new RecipeTreeButtonWidget(bx, y, recipe);
				case DEFAULT -> new RecipeDefaultButtonWidget(bx, y, recipe);
				case SCREENSHOT -> new RecipeScreenshotButtonWidget(bx, y, recipe);
			});
		}
	}

	public int getLeftWidth() {
		return 0;
	}

	public int getRightWidth() {
		return 0;
	}

	public int getWidth() {
		int actionWidth = actionButtons.isEmpty() ? 0 : actionButtons.size() * ACTION_BUTTON_STEP - 2;
		return Math.max(width, actionWidth);
	}

	public int getHeight() {
		return contentHeight + actionRowHeight;
	}

	private static enum ButtonType {
		FILL,
		TREE,
		DEFAULT,
		SCREENSHOT
	}
}
