package dev.emi.emi.api.recipe;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.runtime.EmiDrawContext;

/** Backport of the v1.1 render-thread-safe text-layout change. */
public class EmiInfoRecipe implements EmiRecipe {
    private static final int STACK_WIDTH = 6, MAX_STACKS = STACK_WIDTH * 3;
    private static final int PADDING = 4;
    private static final Minecraft CLIENT = Minecraft.getInstance();

    private final List<EmiIngredient> stacks;
    // Font.split may bake glyphs. Never run it on the async reload worker.
    private final List<Component> sourceText;
    private volatile List<FormattedCharSequence> text;
    private final Identifier id;

    public EmiInfoRecipe(List<EmiIngredient> stacks, List<Component> text, @Nullable Identifier id) {
        this.stacks = stacks;
        this.sourceText = List.copyOf(text);
        this.id = id;
    }

    private List<FormattedCharSequence> wrappedText() {
        List<FormattedCharSequence> lines = text;
        if (lines == null && RenderSystem.isOnRenderThread()) {
            lines = sourceText.stream()
                .flatMap(t -> CLIENT.font.split(t, getDisplayWidth() - 4).stream())
                .toList();
            text = lines;
        }
        return lines == null ? List.of() : lines;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VanillaEmiRecipeCategories.INFO;
    }

    @Override
    public @Nullable Identifier getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return stacks;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return stacks.stream().flatMap(ing -> ing.getEmiStacks().stream()).toList();
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public int getDisplayWidth() {
        return 144;
    }

    @Override
    public int getDisplayHeight() {
        int stackCount = Math.min(stacks.size(), MAX_STACKS);
        int stackHeight = stackCount == 0 ? 0 : ((stackCount - 1) / STACK_WIDTH + 1) * 18;
        int lineCount = RenderSystem.isOnRenderThread()
            ? wrappedText().size()
            : (text == null ? sourceText.size() : text.size());
        return stackHeight + CLIENT.font.lineHeight * lineCount + PADDING;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int stackCount = Math.min(stacks.size(), MAX_STACKS);
        int stackHeight = stackCount == 0 ? 0 : ((stackCount - 1) / STACK_WIDTH + 1);
        int xOff = 54 - (stackCount % STACK_WIDTH * 9);
        for (int i = 0; i < stackCount; i++) {
            int y = i / STACK_WIDTH * 18;
            int x = (i % STACK_WIDTH) * 18;
            if (y / 18 + 1 == stackHeight && stackCount % STACK_WIDTH != 0) {
                x += xOff;
            }
            if (i + 1 == stackCount && stacks.size() > stackCount) {
                widgets.addSlot(EmiIngredient.of(stacks.subList(i, stacks.size())), x + 18, y);
            } else {
                widgets.addSlot(stacks.get(i), x + 18, y);
            }
        }
        List<FormattedCharSequence> lines = wrappedText();
        int y = stackHeight * 18 + PADDING;
        int lineCount = Math.max(0, (widgets.getHeight() - y) / CLIENT.font.lineHeight);
        PageManager manager = new PageManager(lines, lineCount);
        if (lineCount > 0 && lineCount < lines.size()) {
            widgets.addButton(2, 2, 12, 12, 0, 0, () -> true, (mouseX, mouseY, button) -> manager.scroll(-1));
            widgets.addButton(widgets.getWidth() - 14, 2, 12, 12, 12, 0, () -> true, (mouseX, mouseY, button) -> manager.scroll(1));
        }
        widgets.addDrawable(0, y, 0, 0, (raw, mouseX, mouseY, delta) -> {
            EmiDrawContext context = EmiDrawContext.wrap(raw);
            int lo = manager.start();
            for (int i = 0; i < lineCount; i++) {
                int l = lo + i;
                if (l >= manager.lines.size()) {
                    return;
                }
                context.drawText(manager.lines.get(l), 0, i * CLIENT.font.lineHeight, 0);
            }
        });
    }

    private static class PageManager {
        public final List<FormattedCharSequence> lines;
        public final int pageSize;
        public int currentPage;

        public PageManager(List<FormattedCharSequence> lines, int pageSize) {
            this.lines = lines;
            this.pageSize = pageSize;
        }

        public void scroll(int delta) {
            if (pageSize <= 0) {
                return;
            }
            currentPage += delta;
            int totalPages = (lines.size() - 1) / pageSize + 1;
            if (currentPage < 0) {
                currentPage = totalPages - 1;
            }
            if (currentPage >= totalPages) {
                currentPage = 0;
            }
        }

        public int start() {
            return currentPage * pageSize;
        }
    }
}
