package dev.emi.emi.platform.neoforge;


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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = "emi", value = Dist.CLIENT)
public class EmiClientNeoForge {
	
	@SubscribeEvent
	public static void clientInit(FMLClientSetupEvent event) {
		EmiClient.init();
		EmiNetwork.initClient(packet -> ClientPacketDistributor.sendToServer(EmiPacketHandler.wrap(packet)));
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::tagsReloaded);
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::renderScreenForeground);
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::postRenderScreen);
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::mousePressed);
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::mouseReleased);
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::mouseDragged);
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::mouseScrolled);
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::keyPressed);
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::characterTyped);
		ModList.get().getModContainerById("emi").orElseThrow().registerExtensionPoint(IConfigScreenFactory.class,
				(container, last) -> new ConfigScreen(last));
	}


	@SubscribeEvent
	public static void registerResourceReloaders(AddClientReloadListenersEvent event) {
		EmiData.init(reloader -> event.addListener(reloader.getEmiId(), reloader));
	}

	public static void tagsReloaded(TagsUpdatedEvent event) {
		EmiReloadManager.reloadTags();
	}

	public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
		if (event.getScreen() instanceof HandledScreen<?> && EmiScreenManager.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
			event.setCanceled(true);
		}
	}

	public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
		if (event.getScreen() instanceof HandledScreen<?> && EmiScreenManager.mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton())) {
			event.setCanceled(true);
		}
	}

	public static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
		if (event.getScreen() instanceof HandledScreen<?>) {
			EmiScreenManager.mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY());
		}
	}

	public static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
		if (event.getScreen() instanceof HandledScreen<?> && EmiScreenManager.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
			event.setCanceled(true);
		}
	}

	public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
		if (event.getScreen() instanceof HandledScreen<?> && EmiScreenManager.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
			event.setCanceled(true);
		}
	}

	public static void characterTyped(ScreenEvent.CharacterTyped.Pre event) {
		if (event.getScreen() instanceof HandledScreen<?> && EmiScreenManager.search.charTyped(event.getCodePoint(), event.getModifiers())) {
			event.setCanceled(true);
		}
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
			EmiScreenManager.render(context, event.getMouseX(), event.getMouseY(), client.getRenderTickCounter().getTickProgress(false));
			context.pop();
		}
	}

	public static void postRenderScreen(ScreenEvent.Render.Post event) {
		EmiDrawContext context = EmiDrawContext.wrap(event.getGuiGraphics());
		Screen screen = event.getScreen();
		if (!(screen instanceof HandledScreen<?>)) {
			return;
		}
		EmiScreenBase base = EmiScreenBase.of(screen);
		if (base != null) {
			MinecraftClient client = MinecraftClient.getInstance();
			context.push();
			EmiPort.setPositionTexShader();
			EmiScreenManager.drawForeground(context, event.getMouseX(), event.getMouseY(), client.getRenderTickCounter().getTickProgress(false));
			context.pop();
		}
	}
}
