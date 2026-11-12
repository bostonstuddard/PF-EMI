package dev.emi.emi.api.forge;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraftforge.fluids.FluidStack;

public final class ForgeEmiStack {
    private ForgeEmiStack() {}

    public static EmiStack of(FluidStack stack) {
        // Forge FluidStack still stores a legacy CompoundTag in 26.1.2;
        // unlike NeoForge FluidStack, it does not expose DataComponentPatch.
        return EmiStack.of(stack.getFluid(), DataComponentPatch.EMPTY, stack.getAmount());
    }
}
