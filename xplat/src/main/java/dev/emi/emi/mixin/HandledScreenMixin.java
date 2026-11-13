package dev.emi.emi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen {
    @Shadow
    protected int imageWidth, imageHeight, leftPos, topPos;

    private HandledScreenMixin() { super(null, null, null); }

    // 26.3 GUI events can be dispatched through AbstractContainerScreen rather
    // than reaching the legacy MouseHandler/KeyboardHandler invoke targets.
    // Handle the actual inventory-screen methods as a fallback. The existing
    // platform handlers get first chance to consume input; if they do, these
    // injections are never reached. A non-handled event still goes to vanilla.
    // Do not handle keys while the EMI search box is focused: typing U/R there
    // is intentionally text input. Click outside it or press Enter first.
    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void pfemi$inventoryMousePressed(MouseButtonEvent event, boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if (EmiScreenManager.mouseClicked(event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void pfemi$inventoryMouseReleased(MouseButtonEvent event,
            CallbackInfoReturnable<Boolean> cir) {
        if (EmiScreenManager.mouseReleased(event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void pfemi$inventoryKeyPressed(KeyEvent event,
            CallbackInfoReturnable<Boolean> cir) {
        if (EmiScreenManager.keyPressed(event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(at = @At(value = "INVOKE",
            target = "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            shift = Shift.AFTER),
            method = "extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V")
    private void renderForeground(GuiGraphicsExtractor raw, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (EmiAgnos.isForge()) {
            return;
        }
        EmiDrawContext context = EmiDrawContext.wrap(raw);
        context.push();
        context.matrices().translate(-leftPos, -topPos);
        EmiScreenManager.render(context, mouseX, mouseY, delta);
        context.pop();
    }

    @Inject(at = @At("TAIL"),
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V")
    private void renderTail(GuiGraphicsExtractor raw, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (EmiAgnos.isForge()) {
            return;
        }
        EmiDrawContext context = EmiDrawContext.wrap(raw);
        context.push();
        // Run after extractContents/extractCarriedItem/extractSnapbackItem/extractTooltip
        // so the dragged stack's nextStratum() lands above slot items and the
        // vanilla carried item — matching NeoForge's ScreenEvent.Render.Post path.
        EmiScreenManager.drawForeground(context, mouseX, mouseY, delta);
        context.pop();
        context.flushDeferredTooltips();
    }
}
