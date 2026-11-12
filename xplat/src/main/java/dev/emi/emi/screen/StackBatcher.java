package dev.emi.emi.screen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.emi.emi.api.stack.EmiIngredient;

/**
 * Minecraft 26.2's feature-rendering pipeline no longer provides MultiBufferSource
 * or the legacy immediate RenderType drawing contract. Rendering each ingredient
 * through the supported GuiGraphicsExtractor preserves icons and overlays while
 * batched rendering is migrated to SubmitNodeStorage in a later release.
 */
public final class StackBatcher {
    public static final List<net.minecraft.client.renderer.rendertype.RenderType> EXTRA_RENDER_LAYERS = new ArrayList<>();

    public interface Batchable {
        boolean isSideLit();
        boolean isUnbatchable();
        void setUnbatchable();
    }

    public static boolean isEnabled() { return false; }
    public boolean isPopulated() { return false; }
    public void repopulate() { }
    public void begin(int x, int y, int z) { }
    public void draw() { }

    public void render(Batchable item, GuiGraphicsExtractor draw, int x, int y, float delta) {
        if (item instanceof EmiIngredient ingredient) {
            ingredient.render(draw, x, y, delta);
        }
    }

    public void render(EmiIngredient ingredient, GuiGraphicsExtractor draw, int x, int y, float delta) {
        render(ingredient, draw, x, y, delta, -1 ^ EmiIngredient.RENDER_AMOUNT);
    }

    public void render(EmiIngredient ingredient, GuiGraphicsExtractor draw, int x, int y, float delta, int flags) {
        ingredient.render(draw, x, y, delta, flags);
    }

    public static final class ClaimedCollection {
        private final Set<StackBatcher> claimed = new HashSet<>();
        private final List<StackBatcher> unclaimed = new ArrayList<>();
        public synchronized StackBatcher claim() {
            StackBatcher batcher = unclaimed.isEmpty() ? new StackBatcher() : unclaimed.remove(unclaimed.size()-1);
            claimed.add(batcher);
            return batcher;
        }
        public synchronized void unclaim(StackBatcher batcher) {
            if (claimed.remove(batcher)) unclaimed.add(batcher);
        }
        public synchronized void unclaimAll() {
            unclaimed.addAll(claimed);
            claimed.clear();
        }
    }
}
