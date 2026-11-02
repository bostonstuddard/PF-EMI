package dev.emi.emi.platform.fabric;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


import dev.emi.emi.data.EmiData;
import dev.emi.emi.network.CommandS2CPacket;
import dev.emi.emi.network.EmiChessPacket;
import dev.emi.emi.network.EmiNetwork;
import dev.emi.emi.network.EmiPacket;
import dev.emi.emi.network.PingS2CPacket;
import dev.emi.emi.network.RecipeSyncS2CPacket;
import dev.emi.emi.platform.EmiClient;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketDecoder;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public class EmiClientFabric implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EmiClient.init();
		EmiData.init(reloader -> {
			ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {

				@Override
				public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager,
						Executor prepareExecutor, Executor applyExecutor) {
					return reloader.reload(synchronizer, manager, prepareExecutor, applyExecutor);
				}

				@Override
				public String getName() {
					return reloader.getName();
				}

				@Override
				public Identifier getFabricId() {
					return reloader.getEmiId();
				}
			});
		});


		EmiNetwork.initClient(packet -> {
			if (ClientPlayNetworking.canSend(packet.getId())) {
				ClientPlayNetworking.send(packet);
			}
		});

		registerPacketReader(EmiNetwork.PING, PingS2CPacket::new);
		registerPacketReader(EmiNetwork.COMMAND, CommandS2CPacket::new);
		registerPacketReader(EmiNetwork.CHESS, EmiChessPacket.S2C::new);
		registerPacketReader(EmiNetwork.RECIPE_SYNC, RecipeSyncS2CPacket::new);
	}

	private <T extends EmiPacket> void registerPacketReader(CustomPayload.Id<T> id, PacketDecoder<RegistryByteBuf, T> decode) {
		ClientPlayNetworking.registerGlobalReceiver(id, (payload, context) -> {
			context.client().execute(() -> {
				payload.apply(context.client().player);
			});
		});
	}
}
