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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "emi", value = Dist.CLIENT)
public class EmiClientNeoForge {
	
	@SubscribeEvent
	public static void clientInit(FMLClientSetupEvent event) {
		EmiClient.init();
		EmiNetwork.initClient(packet -> ClientPacketDistributor.sendToServer(EmiPacketHandler.wrap(packet)));
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::tagsReloaded);
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::recipesReceived);
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::playerLoggedOut);
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::renderScreenForeground);
		NeoForge.EVENT_BUS.addListener(EmiClientNeoForge::postRenderScreen);
		ModList.get().getModContainerById("emi").orElseThrow().registerExtensionPoint(IConfigScreenFactory.class,
				(container, last) -> new ConfigScreen(last));
	}

	@SubscribeEvent
	public static void registerResourceReloaders(AddClientReloadListenersEvent event) {
		EmiData.init(reloader -> event.addListener(reloader.getEmiId(), reloader));
	}

	public static void tagsReloaded(TagsUpdatedEvent event) {
		Minecraft client = Minecraft.getInstance();
		// A vanilla/non-NeoForge server can send tag updates without the
		// recipe map required by EMI. Don't schedule an impossible reload.
		// A later RecipesReceivedEvent will start it once a map exists.
		if (client.level != null && EmiPort.getRecipeMap() != null) {
			EmiReloadManager.reloadRecipes();
			EmiReloadManager.reloadTags();
		}
	}

	public static void recipesReceived(RecipesReceivedEvent event) {
		EmiAgnosNeoForge.setReceivedRecipeMap(event.getRecipeMap());
		if (event.getRecipeMap() != null) {
			EmiReloadManager.reloadRecipes();
			EmiReloadManager.reloadTags();
		}
	}

	public static void playerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
		EmiAgnosNeoForge.setReceivedRecipeMap(null);
	}

	public static void renderScreenForeground(ScreenEvent.Render.Foreground event) {
		if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
			return;
		}
		EmiDrawContext context = EmiDrawContext.wrap(event.getGuiGraphics());
		EmiScreenBase base = EmiScreenBase.of(screen);
		if (base != null) {
			Minecraft client = Minecraft.getInstance();
			context.push();
			// ScreenEvent.Render.Foreground is dispatched after extractContents() restores
			// the GUI pose, so EMI renders in full-screen coordinates here.
			// Do not subtract screen.getLeftPos()/getTopPos(): doing so shifts
			// the entire sidebar over the inventory on NeoForge 26.2.
			EmiScreenManager.render(context, event.getMouseX(), event.getMouseY(), client.getDeltaTracker().getGameTimeDeltaPartialTick(false));
			context.pop();
		}
	}

	public static void postRenderScreen(ScreenEvent.Render.Post event) {
		EmiDrawContext context = EmiDrawContext.wrap(event.getGuiGraphics());
		Screen screen = event.getScreen();
		if (!(screen instanceof AbstractContainerScreen<?>)) {
			return;
		}
		EmiScreenBase base = EmiScreenBase.of(screen);
		if (base != null) {
			Minecraft client = Minecraft.getInstance();
			context.push();
			EmiScreenManager.drawForeground(context, event.getMouseX(), event.getMouseY(), client.getDeltaTracker().getGameTimeDeltaPartialTick(false));
			context.pop();
		}
		context.flushDeferredTooltips();
	}
}
