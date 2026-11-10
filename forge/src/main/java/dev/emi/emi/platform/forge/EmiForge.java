package dev.emi.emi.platform.forge;

import dev.emi.emi.network.EmiNetwork;
import dev.emi.emi.network.PingS2CPacket;
import dev.emi.emi.platform.EmiMain;
import dev.emi.emi.registry.EmiCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod("emi")
public class EmiForge {
    public EmiForge(FMLJavaModLoadingContext context) {
        EmiMain.init();
        EmiPacketHandler.init();
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event -> EmiClientForge.init());
        }
        EmiNetwork.initServer((player, packet) -> {
            if (EmiPacketHandler.CHANNEL.isRemotePresent(player.connection.getConnection())) {
                EmiPacketHandler.CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
            }
        });
        RegisterCommandsEvent.BUS.addListener(event -> EmiCommands.registerCommands(event.getDispatcher()));
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                EmiNetwork.sendToClient(player, new PingS2CPacket());
            }
        });
        OnDatapackSyncEvent.BUS.addListener(EmiRecipeSync::sendToPlayers);
    }
}
