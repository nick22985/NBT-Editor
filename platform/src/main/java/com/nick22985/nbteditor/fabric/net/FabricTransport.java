package com.nick22985.nbteditor.fabric.net;

import com.nick22985.nbteditor.net.Transport;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?} else {
/*import com.nick22985.nbteditor.fabric.util.McIdUtil;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
*///?}

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricTransport implements Transport {
	//? if <1.20.5
	//private static final Identifier CHANNEL = McIdUtil.of("nbtedit", "frame");

	private final Supplier<MinecraftServer> server;
	private FabricClientChannel client;
	private BiConsumer<UUID, byte[]> serverHandler = (player, frame) -> {
	};
	private Consumer<byte[]> clientHandler = frame -> {
	};

	public FabricTransport(Supplier<MinecraftServer> server) {
		this.server = server;
	}

	public void client(FabricClientChannel client) {
		this.client = client;
		client.register(this::receiveClient);
	}

	public void registerCommon() {
		//? if >=26.1 {
		PayloadTypeRegistry.serverboundPlay().register(FramePayload.TYPE, FramePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(FramePayload.TYPE, FramePayload.CODEC);
		//?} else if >=1.20.5 {
		/*PayloadTypeRegistry.playC2S().register(FramePayload.TYPE, FramePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FramePayload.TYPE, FramePayload.CODEC);
		*///?}
		//? if >=1.20.5 {
		ServerPlayNetworking.registerGlobalReceiver(FramePayload.TYPE, (payload, context) -> this.receiveServer(context.player().getUUID(), payload.data()));
		//?} else {
		/*ServerPlayNetworking.registerGlobalReceiver(CHANNEL, (server, player, handler, buf, sender) -> {
			byte[] data = buf.readByteArray(MAX_SERVERBOUND_FRAME);
			UUID uuid = player.getUUID();
			server.execute(() -> this.receiveServer(uuid, data));
		});
		*///?}
	}

	public void receiveServer(UUID player, byte[] frame) {
		if (frame.length <= MAX_SERVERBOUND_FRAME) {
			this.serverHandler.accept(player, frame);
		}
	}

	public void receiveClient(byte[] frame) {
		this.clientHandler.accept(frame);
	}

	private ServerPlayer player(UUID uuid) {
		MinecraftServer server = this.server.get();
		return server == null ? null : server.getPlayerList().getPlayer(uuid);
	}

	@Override
	public boolean serverHasChannel() {
		return this.client != null && this.client.available();
	}

	@Override
	public boolean clientHasChannel(UUID uuid) {
		ServerPlayer player = this.player(uuid);
		//? if >=1.20.5 {
		return player != null && ServerPlayNetworking.canSend(player, FramePayload.TYPE);
		//?} else
		//return player != null && ServerPlayNetworking.canSend(player, CHANNEL);
	}

	@Override
	public void sendToServer(byte[] frame) {
		if (this.client != null) {
			this.client.send(frame);
		}
	}

	@Override
	public void sendToClient(UUID uuid, byte[] frame) {
		ServerPlayer player = this.player(uuid);
		if (player == null) {
			return;
		}
		//? if >=1.20.5 {
		ServerPlayNetworking.send(player, new FramePayload(frame));
		//?} else {
		/*FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeByteArray(frame);
		ServerPlayNetworking.send(player, CHANNEL, buf);
		*///?}
	}

	@Override
	public void onServerFrame(BiConsumer<UUID, byte[]> handler) {
		this.serverHandler = handler;
	}

	@Override
	public void onClientFrame(Consumer<byte[]> handler) {
		this.clientHandler = handler;
	}
}
