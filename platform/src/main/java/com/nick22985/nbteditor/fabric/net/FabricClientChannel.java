package com.nick22985.nbteditor.fabric.net;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if <1.20.5 {
/*import com.nick22985.nbteditor.fabric.util.McIdUtil;
import com.nick22985.nbteditor.net.Transport;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
*///?}

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricClientChannel {
	//? if <1.20.5
	//private static final Identifier CHANNEL = McIdUtil.of("nbtedit", "frame");

	public void register(Consumer<byte[]> receiver) {
		//? if >=1.20.5 {
		ClientPlayNetworking.registerGlobalReceiver(FramePayload.TYPE, (payload, context) -> receiver.accept(payload.data()));
		//?} else {
		/*ClientPlayNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, sender) -> {
			byte[] data = buf.readByteArray(Transport.MAX_CLIENTBOUND_FRAME);
			client.execute(() -> receiver.accept(data));
		});
		*///?}
	}

	public boolean available() {
		//? if >=1.20.5 {
		return ClientPlayNetworking.canSend(FramePayload.TYPE);
		//?} else
		//return ClientPlayNetworking.canSend(CHANNEL);
	}

	public void send(byte[] frame) {
		//? if >=1.20.5 {
		ClientPlayNetworking.send(new FramePayload(frame));
		//?} else {
		/*FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeByteArray(frame);
		ClientPlayNetworking.send(CHANNEL, buf);
		*///?}
	}
}
