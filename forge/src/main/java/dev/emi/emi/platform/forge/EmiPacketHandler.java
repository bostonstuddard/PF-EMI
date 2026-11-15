package dev.emi.emi.platform.forge;

import dev.emi.emi.EmiPort;
import dev.emi.emi.network.CommandS2CPacket;
import dev.emi.emi.network.CreateItemC2SPacket;
import dev.emi.emi.network.EmiChessPacket;
import dev.emi.emi.network.EmiNetwork;
import dev.emi.emi.network.EmiPacket;
import dev.emi.emi.network.FillRecipeC2SPacket;
import dev.emi.emi.network.PingS2CPacket;
import dev.emi.emi.network.RecipeSyncS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketDecoder;
import net.minecraft.network.packet.CustomPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;

public final class EmiPacketHandler {
	private static final CustomPayload.Id<EmiChessPacket.S2C> ID_CHESS_CLIENTBOUND =
		new CustomPayload.Id<>(EmiPort.id("emi:chess_s2c"));
	private static final CustomPayload.Id<EmiChessPacket.C2S> ID_CHESS_SERVERBOUND =
		new CustomPayload.Id<>(EmiPort.id("emi:chess_c2s"));

	public static final Channel<CustomPayload> CHANNEL = createChannel();

	private EmiPacketHandler() {
	}

	private static Channel<CustomPayload> createChannel() {
		var connection = ChannelBuilder.named(EmiPort.id("emi:emi"))
			.networkProtocolVersion(1)
			.optional()
			.payloadChannel();
		var play = connection.play().bidirectional();

		play.addMain(EmiNetwork.FILL_RECIPE, makeReader(EmiNetwork.FILL_RECIPE, FillRecipeC2SPacket::new), EmiPacketHandler::handleServerbound);
		play.addMain(EmiNetwork.CREATE_ITEM, makeReader(EmiNetwork.CREATE_ITEM, CreateItemC2SPacket::new), EmiPacketHandler::handleServerbound);
		play.addMain(ID_CHESS_SERVERBOUND, makeReader(ID_CHESS_SERVERBOUND, EmiChessPacket.C2S::new), EmiPacketHandler::handleServerbound);

		play.addMain(EmiNetwork.PING, makeReader(EmiNetwork.PING, PingS2CPacket::new), EmiPacketHandler::handleClientbound);
		play.addMain(EmiNetwork.COMMAND, makeReader(EmiNetwork.COMMAND, CommandS2CPacket::new), EmiPacketHandler::handleClientbound);
		play.addMain(EmiNetwork.RECIPE_SYNC, makeReader(EmiNetwork.RECIPE_SYNC, RecipeSyncS2CPacket::new), EmiPacketHandler::handleClientbound);
		play.addMain(ID_CHESS_CLIENTBOUND, makeReader(ID_CHESS_CLIENTBOUND, EmiChessPacket.S2C::new), EmiPacketHandler::handleClientbound);

		return play.build();
	}

	public static void init() {
		// Forces static channel creation during mod initialization.
	}

	private static <T extends EmiPacket> PacketCodec<RegistryByteBuf, T> makeReader(
			CustomPayload.Id<T> id, PacketDecoder<RegistryByteBuf, T> reader) {
		return PacketCodec.of(EmiPacket::write, reader);
	}

	private static void handleServerbound(EmiPacket packet, CustomPayloadEvent.Context context) {
		if (!context.isServerSide()) {
			throw new IllegalArgumentException("Trying to handle serverbound EMI packet on client: " + packet);
		}
		var player = context.getSender();
		if (player != null) {
			packet.apply(player);
		}
	}

	private static void handleClientbound(EmiPacket packet, CustomPayloadEvent.Context context) {
		if (!context.isClientSide()) {
			throw new IllegalArgumentException("Trying to handle clientbound EMI packet on server: " + packet);
		}
		// All PF-EMI clientbound packets are client-state packets and do not require
		// a non-null PlayerEntity. Do not silently drop login-time packets just
		// because MinecraftClient.player has not been installed yet.
		packet.apply(MinecraftClient.getInstance().player);
	}
}
