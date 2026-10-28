package dev.emi.emi.mixin;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.config.SidebarSide;
import dev.emi.emi.screen.RecipeScreen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PF-EMI responsive safety clamps.
 *
 * RecipeDisplay handles recipe-action controls; this mixin prevents the outer
 * screen, category tabs, and workstation strip from being positioned beyond
 * the physical window on small GUI sizes.
 *
 * PF_EMI_SCREEN_BOUNDS_pf.6
 */
@Mixin(RecipeScreen.class)
public abstract class RecipeScreenMixin {
	@Shadow private int minimumWidth;
	@Shadow int backgroundHeight;

	private int pf$horizontalReserve() {
		return EmiConfig.workstationLocation == SidebarSide.LEFT
			|| EmiConfig.workstationLocation == SidebarSide.RIGHT ? 40 : 12;
	}

	@Inject(
		method = "init",
		at = @At(
			value = "FIELD",
			target = "Ldev/emi/emi/screen/RecipeScreen;minimumWidth:I",
			opcode = Opcodes.PUTFIELD,
			shift = At.Shift.AFTER
		),
		require = 0
	)
	private void pf$clampMinimumWidth(CallbackInfo ci) {
		RecipeScreen screen = (RecipeScreen) (Object) this;
		int max = Math.max(56, screen.width - pf$horizontalReserve());
		this.minimumWidth = Math.max(56, Math.min(this.minimumWidth, max));
	}

	@Inject(
		method = "init",
		at = @At(
			value = "FIELD",
			target = "Ldev/emi/emi/screen/RecipeScreen;backgroundHeight:I",
			opcode = Opcodes.PUTFIELD,
			shift = At.Shift.AFTER
		),
		require = 0
	)
	private void pf$clampBackgroundHeight(CallbackInfo ci) {
		RecipeScreen screen = (RecipeScreen) (Object) this;
		int max = Math.max(54, screen.height - 58);
		this.backgroundHeight = Math.max(54, Math.min(this.backgroundHeight, max));
	}

	@ModifyVariable(
		method = "setRecipePageWidth",
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 0,
		require = 0
	)
	private int pf$clampRecipePageWidth(int requestedWidth) {
		RecipeScreen screen = (RecipeScreen) (Object) this;
		int max = Math.max(56, screen.width - pf$horizontalReserve());
		return Math.max(56, Math.min(requestedWidth, max));
	}

	@Inject(method = "getWorkstationBounds", at = @At("RETURN"), cancellable = true, require = 0)
	private void pf$clampWorkstationBounds(int index, CallbackInfoReturnable<Bounds> cir) {
		Bounds bounds = cir.getReturnValue();
		if (bounds == null || bounds == Bounds.EMPTY) {
			return;
		}
		RecipeScreen screen = (RecipeScreen) (Object) this;
		int x = Math.max(4, Math.min(bounds.x(), Math.max(4, screen.width - bounds.width() - 4)));
		int y = Math.max(4, Math.min(bounds.y(), Math.max(4, screen.height - bounds.height() - 4)));
		if (x != bounds.x() || y != bounds.y()) {
			cir.setReturnValue(new Bounds(x, y, bounds.width(), bounds.height()));
		}
	}

	@Inject(method = "getBounds", at = @At("RETURN"), cancellable = true, require = 0)
	private void pf$clampOverallBounds(CallbackInfoReturnable<Bounds> cir) {
		Bounds bounds = cir.getReturnValue();
		if (bounds == null) {
			return;
		}
		RecipeScreen screen = (RecipeScreen) (Object) this;
		int left = Math.max(0, bounds.x());
		int top = Math.max(0, bounds.y());
		int right = Math.min(screen.width, bounds.x() + bounds.width());
		int bottom = Math.min(screen.height, bounds.y() + bounds.height());
		cir.setReturnValue(new Bounds(
			left,
			top,
			Math.max(0, right - left),
			Math.max(0, bottom - top)
		));
	}
}
