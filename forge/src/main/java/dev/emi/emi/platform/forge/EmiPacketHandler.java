package dev.emi.emi.platform.forge;

import dev.emi.emi.network.CommandS2CPacket;
import dev.emi.emi.network.CreateItemC2SPacket;
import dev.emi.emi.network.EmiChessPacket;
import dev.emi.emi.network.FillRecipeC2SPacket;
import dev.emi.emi.network.PingS2CPacket;
import dev.emi.emi.network.EmiPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;

import java.util.function.BiConsumer;

public final class EmiPacketHandler {
    public static final SimpleChannel CHANNEL = ChannelBuilder.named("emi:emi").optional().simpleChannel();

    private EmiPacketHandler() {}

    public static void init() {
        int i = 0;
        CHANNEL.messageBuilder(FillRecipeC2SPacket.class, i++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(FillRecipeC2SPacket::write).decoder(FillRecipeC2SPacket::new)
            .consumerMainThread(serverHandler(FillRecipeC2SPacket::apply)).add();
        CHANNEL.messageBuilder(CreateItemC2SPacket.class, i++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(CreateItemC2SPacket::write).decoder(CreateItemC2SPacket::new)
            .consumerMainThread(serverHandler(CreateItemC2SPacket::apply)).add();
        CHANNEL.messageBuilder(EmiChessPacket.C2S.class, i++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(EmiChessPacket.C2S::write).decoder(EmiChessPacket.C2S::new)
            .consumerMainThread(serverHandler(EmiChessPacket.C2S::apply)).add();
        CHANNEL.messageBuilder(PingS2CPacket.class, i++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(PingS2CPacket::write).decoder(PingS2CPacket::new)
            .consumerMainThread(clientHandler(PingS2CPacket::apply)).add();
        CHANNEL.messageBuilder(CommandS2CPacket.class, i++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(CommandS2CPacket::write).decoder(CommandS2CPacket::new)
            .consumerMainThread(clientHandler(CommandS2CPacket::apply)).add();
        CHANNEL.messageBuilder(EmiChessPacket.S2C.class, i++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(EmiChessPacket.S2C::write).decoder(EmiChessPacket.S2C::new)
            .consumerMainThread(clientHandler(EmiChessPacket.S2C::apply)).add();
        EmiRecipeSync.register(CHANNEL, i);
    }

    private static <T extends EmiPacket> BiConsumer<T, CustomPayloadEvent.Context> serverHandler(BiConsumer<T, Player> handler) {
        return (packet, context) -> {
            Player player = context.getSender();
            if (player != null) {
                handler.accept(packet, player);
            }
            context.setPacketHandled(true);
        };
    }

    private static <T extends EmiPacket> BiConsumer<T, CustomPayloadEvent.Context> clientHandler(BiConsumer<T, Player> handler) {
        return (packet, context) -> {
            var mc = Minecraft.getInstance();
            if (mc.player != null) {
                handler.accept(packet, mc.player);
            }
            context.setPacketHandled(true);
        };
    }
}
