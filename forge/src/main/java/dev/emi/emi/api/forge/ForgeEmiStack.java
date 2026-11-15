package dev.emi.emi.api.forge;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.component.ComponentChanges;
import net.minecraftforge.fluids.FluidStack;

public final class ForgeEmiStack {
	private ForgeEmiStack() {
	}

	public static EmiStack of(FluidStack stack) {
		return EmiStack.of(stack.getFluid(), ComponentChanges.EMPTY, stack.getAmount());
	}
}
