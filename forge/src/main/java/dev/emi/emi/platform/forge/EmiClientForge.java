package dev.emi.emi.platform.forge;

import dev.emi.emi.EmiPort;
import dev.emi.emi.data.EmiData;
import dev.emi.emi.network.EmiNetwork;
import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.emi.screen.ConfigScreen;
import dev.emi.emi.screen.EmiScreenBase;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "emi", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EmiClientForge {

	@SubscribeEvent
	public static void clientInit(FMLClientSetupEvent event) {
		EmiClient.init();
		EmiNetwork.initClient(packet -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.getNetworkHandler() != null) {
				EmiPacketHandler.CHANNEL.send(packet, client.getNetworkHandler().getConnection());
			}
		});
		TagsUpdatedEvent.BUS.addListener(EmiClientForge::tagsReloaded);
		ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(EmiClientForge::playerLoggedIn);
		ContainerScreenEvent.Render.Foreground.BUS.addListener(EmiClientForge::renderScreenForeground);
		ScreenEvent.MouseButtonPressed.Pre.BUS.addListener(EmiClientForge::mousePressed);
		ScreenEvent.MouseButtonReleased.Pre.BUS.addListener(EmiClientForge::mouseReleased);
		ScreenEvent.MouseDragged.Pre.BUS.addListener(EmiClientForge::mouseDragged);
		ScreenEvent.MouseScrolled.Pre.BUS.addListener(EmiClientForge::mouseScrolled);
		ScreenEvent.KeyPressed.Pre.BUS.addListener(EmiClientForge::keyPressed);
		ScreenEvent.CharacterTyped.Pre.BUS.addListener(EmiClientForge::characterTyped);
		RegisterClientReloadListenersEvent.BUS.addListener(EmiClientForge::registerResourceReloaders);
		MinecraftForge.registerConfigScreen(last -> new ConfigScreen(last));
	}

	public static void registerResourceReloaders(RegisterClientReloadListenersEvent event) {
		EmiData.init(event::registerReloadListener);
	}

	public static void tagsReloaded(TagsUpdatedEvent event) {
		EmiReloadManager.reloadTags();
	}

	public static void playerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
		// Forge does not reliably deliver a client tag-update event for integrated
		// server connections. By the time the local player is installed, login-time
		// registry/tag state is available, so mark the tag half of EMI's reload gate
		// here as well. The normal TagsUpdatedEvent remains registered for dedicated
		// servers and later datapack reloads.
		EmiReloadManager.reloadTags();
	}

	public static boolean mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
		if (event.getScreen() instanceof HandledScreen<?>) {
			return EmiScreenManager.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
		}
		return false;
	}

	public static boolean mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
		if (event.getScreen() instanceof HandledScreen<?>) {
			return EmiScreenManager.mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
		}
		return false;
	}

	public static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
		if (event.getScreen() instanceof HandledScreen<?>) {
			EmiScreenManager.mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY());
		}
	}

	public static boolean mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
		if (event.getScreen() instanceof HandledScreen<?>) {
			return EmiScreenManager.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getDeltaY());
		}
		return false;
	}

	public static boolean keyPressed(ScreenEvent.KeyPressed.Pre event) {
		if (event.getScreen() instanceof HandledScreen<?>) {
			return EmiScreenManager.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
		}
		return false;
	}

	public static boolean characterTyped(ScreenEvent.CharacterTyped.Pre event) {
		if (event.getScreen() instanceof HandledScreen<?>) {
			return EmiScreenManager.search.charTyped(event.getCodePoint(), event.getModifiers());
		}
		return false;
	}

	public static void renderScreenForeground(ContainerScreenEvent.Render.Foreground event) {
		EmiDrawContext context = EmiDrawContext.wrap(event.getGuiGraphics());
		HandledScreen<?> screen = event.getContainerScreen();
		EmiScreenBase base = EmiScreenBase.of(screen);
		if (base != null) {
			MinecraftClient client = MinecraftClient.getInstance();
			context.push();
			context.matrices().translate(-screen.getGuiLeft(), -screen.getGuiTop(), 0.0);
			EmiPort.setPositionTexShader();
			float delta = client.getRenderTickCounter().getTickProgress(false);
			EmiScreenManager.render(context, event.getMouseX(), event.getMouseY(), delta);
			// 1.21.8 defers GUI tooltips into a hover stratum. Submitting EMI's
			// tooltip from ScreenEvent.Render.Post is too late on Forge, so keep
			// drawForeground beside render(), matching the Fabric HandledScreen mixin.
			EmiScreenManager.drawForeground(context, event.getMouseX(), event.getMouseY(), delta);
			context.pop();
		}
	}
}
