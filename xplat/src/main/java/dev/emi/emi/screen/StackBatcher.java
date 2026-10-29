package dev.emi.emi.screen;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;

/**
 * Compatibility shell for EMI's legacy item batcher.
 *
 * <p>Minecraft 1.21.4 replaced the item-model/render pipeline with ItemModelManager
 * and ItemRenderState. PF-EMI already disabled the legacy batcher on 1.21.2 because
 * it did not preserve vanilla GUI render state correctly. Keep batching disabled and
 * route every ingredient through DrawContext, which is the authoritative vanilla path
 * on 1.21.4 as well.</p>
 */
public class StackBatcher {
	public interface Batchable {
		boolean isSideLit();
		boolean isUnbatchable();
		void setUnbatchable();
		void renderForBatch(VertexConsumerProvider vcp, DrawContext draw, int x, int y, int z, float delta);
	}

	public static boolean isEnabled() {
		return false;
	}

	public StackBatcher() {
	}

	public boolean isPopulated() {
		return true;
	}

	public void repopulate() {
	}

	public void begin(int x, int y, int z) {
	}

	public void render(Batchable batchable, DrawContext draw, int x, int y, float delta) {
		if (batchable instanceof EmiIngredient ingredient) {
			ingredient.render(draw, x, y, delta, -1);
		}
	}

	public void render(EmiIngredient stack, DrawContext draw, int x, int y, float delta) {
		render(stack, draw, x, y, delta, -1 ^ EmiIngredient.RENDER_AMOUNT);
	}

	public void render(EmiIngredient stack, DrawContext draw, int x, int y, float delta, int flags) {
		stack.render(draw, x, y, delta, flags);
	}

	public void draw() {
	}

	public static class ClaimedCollection {
		private final Set<StackBatcher> claimed = Sets.newHashSet();
		private final List<StackBatcher> unclaimed = Lists.newArrayList();

		public StackBatcher claim() {
			synchronized (this) {
				StackBatcher batcher = unclaimed.isEmpty() ? new StackBatcher() : unclaimed.remove(unclaimed.size() - 1);
				claimed.add(batcher);
				return batcher;
			}
		}

		public void unclaim(StackBatcher batcher) {
			synchronized (this) {
				claimed.remove(batcher);
				unclaimed.add(batcher);
			}
		}

		public void unclaimAll() {
			synchronized (this) {
				unclaimed.addAll(claimed);
				claimed.clear();
			}
		}
	}
}
