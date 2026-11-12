package dev.emi.emi.platform.forge;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import dev.emi.emi.data.EmiData;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.mixin.accessor.HandledScreenAccessor;
import dev.emi.emi.network.EmiNetwork;
import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.emi.screen.ConfigScreen;
import dev.emi.emi.screen.EmiScreenBase;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.screen.StackBatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.network.PacketDistributor;

/** Loaded through Dist-safe initialization by the common @Mod entrypoint. */
public final class EmiClientForge {
    private EmiClientForge() {}

    // Forge's ScreenEvent.MouseButtonReleased.Pre provides a button number but not
    // the modifiers from the original click. Retain those for EMI's input API.
    private static final Map<Integer, MouseButtonInfo> pressedButtons = new HashMap<>();

    public static void init() {
        StackBatcher.EXTRA_RENDER_LAYERS.addAll(Arrays.stream(ForgeRenderTypes.values()).map(ForgeRenderTypes::get).toList());
        EmiClient.init();
        EmiNetwork.initClient(packet -> EmiPacketHandler.CHANNEL.send(packet, PacketDistributor.SERVER.noArg()));
        MinecraftForge.registerConfigScreen(ConfigScreen::new);

        TagsUpdatedEvent.BUS.addListener(event -> {
            if (Minecraft.getInstance().level != null && EmiAgnosForge.hasReceivedRecipes()) {
                EmiReloadManager.reloadTags();
            }
        });
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> {
            EmiRecipeSync.reset();
            pressedButtons.clear();
        });
        ContainerScreenEvent.Render.Foreground.BUS.addListener(EmiClientForge::renderScreenForeground);
        ScreenEvent.Render.Post.BUS.addListener(EmiClientForge::postRenderScreen);
        // Forge's 26.1.2 GUI input events avoid the shared MouseMixin and
        // KeyboardMixin injection targets, which fail under Forge Mixin 0.8.7.
        ScreenEvent.MouseButtonPressed.Pre.BUS.addListener(EmiClientForge::mousePressed);
        ScreenEvent.MouseButtonReleased.Pre.BUS.addListener(EmiClientForge::mouseReleased);
        ScreenEvent.MouseDragged.Pre.BUS.addListener(EmiClientForge::mouseDragged);
        ScreenEvent.MouseScrolled.Pre.BUS.addListener(EmiClientForge::mouseScrolled);
        ScreenEvent.KeyPressed.Pre.BUS.addListener(EmiClientForge::keyPressed);
        ScreenEvent.CharacterTyped.Pre.BUS.addListener(EmiClientForge::characterTyped);
        RegisterClientReloadListenersEvent.BUS.addListener(event ->
            EmiData.init(reloader -> event.registerReloadListener(reloader)));
    }

    private static boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return false;
        try {
            pressedButtons.put(event.getButton(), event.getInfo().buttonInfo());
            return EmiScreenManager.mouseClicked(event.getInfo());
        } catch (Exception e) {
            EmiLog.error("Error while handling Forge GUI mouse press", e);
            return false;
        }
    }

    private static boolean mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        MouseButtonInfo info = pressedButtons.remove(event.getButton());
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return false;
        try {
            if (info == null) info = new MouseButtonInfo(event.getButton(), 0);
            return EmiScreenManager.mouseReleased(new MouseButtonEvent(event.getMouseX(), event.getMouseY(), info));
        } catch (Exception e) {
            EmiLog.error("Error while handling Forge GUI mouse release", e);
            return false;
        }
    }

    private static boolean mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return false;
        try {
            MouseButtonInfo info = pressedButtons.get(event.getMouseButton());
            if (info == null) info = new MouseButtonInfo(event.getMouseButton(), 0);
            return EmiScreenManager.mouseDragged(
                    new MouseButtonEvent(event.getMouseX(), event.getMouseY(), info),
                    event.getDragX(), event.getDragY());
        } catch (Exception e) {
            EmiLog.error("Error while handling Forge GUI mouse drag", e);
            return false;
        }
    }

    private static boolean mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return false;
        try {
            Minecraft mc = Minecraft.getInstance();
            double vertical = event.getDeltaY();
            double amount = (mc.options.discreteMouseScroll().get() ? Math.signum(vertical) : vertical)
                    * mc.options.mouseWheelSensitivity().get();
            return EmiScreenManager.mouseScrolled(event.getMouseX(), event.getMouseY(), amount);
        } catch (Exception e) {
            EmiLog.error("Error while handling Forge GUI mouse scroll", e);
            return false;
        }
    }

    private static boolean keyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return false;
        try {
            return EmiScreenManager.keyPressed(event.getInfo());
        } catch (Exception e) {
            EmiLog.error("Error while handling Forge GUI key press", e);
            return false;
        }
    }

    private static boolean characterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return false;
        if (Minecraft.getInstance().gui.overlay() != null) return false;
        try {
            return EmiScreenManager.search.charTyped(event.getInfo());
        } catch (Exception e) {
            EmiLog.error("Error while handling Forge GUI text input", e);
            return false;
        }
    }

    private static void renderScreenForeground(ContainerScreenEvent.Render.Foreground event) {
        EmiDrawContext ctx = EmiDrawContext.wrap(event.getGuiGraphics());
        var screen = event.getContainerScreen();
        if (EmiScreenBase.of(screen) != null) {
            ctx.push();
            HandledScreenAccessor position = (HandledScreenAccessor) screen;
            ctx.matrices().translate(-position.getX(), -position.getY());
            EmiScreenManager.render(ctx, event.getMouseX(), event.getMouseY(), Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
            ctx.pop();
        }
    }

    private static void postRenderScreen(ScreenEvent.Render.Post event) {
        EmiDrawContext ctx = EmiDrawContext.wrap(event.getGuiGraphics());
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?>)) return;
        if (EmiScreenBase.of(screen) != null) {
            ctx.push();
            EmiScreenManager.drawForeground(ctx, event.getMouseX(), event.getMouseY(), Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
            ctx.pop();
        }
        ctx.flushDeferredTooltips();
    }
}
