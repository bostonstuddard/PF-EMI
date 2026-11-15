package dev.emi.emi.platform.forge;

import dev.emi.emi.network.EmiNetwork;
import dev.emi.emi.network.PingS2CPacket;
import dev.emi.emi.network.RecipeSyncS2CPacket;
import dev.emi.emi.platform.EmiMain;
import dev.emi.emi.registry.EmiCommands;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;

@Mod("emi")
public class EmiForge {

	public EmiForge(FMLJavaModLoadingContext context) {
		EmiMain.init();
		EmiPacketHandler.init();
		EmiNetwork.initServer((player, packet) -> {
			if (EmiPacketHandler.CHANNEL.isRemotePresent(player.networkHandler.getConnection())) {
				EmiPacketHandler.CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
			} else {
				EmiLog.warn("Can't send EMI packet to " + player + " as they're missing the channel");
			}
		});
		RegisterCommandsEvent.BUS.addListener(this::registerCommands);
		PlayerEvent.PlayerLoggedInEvent.BUS.addListener(this::playerConnect);
		OnDatapackSyncEvent.BUS.addListener(this::dataPackSync);
	}

	public void registerCommands(RegisterCommandsEvent event) {
		EmiCommands.registerCommands(event.getDispatcher());
	}

	public void playerConnect(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayerEntity spe) {
			EmiNetwork.sendToClient(spe, new PingS2CPacket());
			// Forge fires OnDatapackSyncEvent before the normal login recipe/tag packets.
			// Sending PF-EMI's full recipe objects there can reach the client before its
			// play-side player is ready. Send the initial recipe snapshot after the
			// player has actually finished logging in instead.
			EmiNetwork.sendToClient(spe, new RecipeSyncS2CPacket(spe.getEntityWorld().getServer().getRecipeManager().values()));
		}
	}

	public void dataPackSync(OnDatapackSyncEvent event) {
		// A non-null player here is the pre-login sync handled above by
		// PlayerLoggedInEvent. A null player means a global datapack reload, where
		// all connected clients do need a fresh authoritative recipe snapshot.
		if (event.getPlayer() == null) {
			event.getPlayers().forEach(player ->
				EmiNetwork.sendToClient(player, new RecipeSyncS2CPacket(player.getEntityWorld().getServer().getRecipeManager().values())));
		}
	}
}
