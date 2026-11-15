package dev.emi.emi.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.fog.FogRenderer;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
	@Accessor("guiRenderer")
	GuiRenderer emi$getGuiRenderer();

	@Accessor("guiState")
	GuiRenderState emi$getGuiState();

	@Accessor("fogRenderer")
	FogRenderer emi$getFogRenderer();
}
