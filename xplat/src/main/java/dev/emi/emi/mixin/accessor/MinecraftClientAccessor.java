package dev.emi.emi.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
	@Mutable
	@Accessor("framebuffer")
	void emi$setFramebuffer(Framebuffer framebuffer);
}
