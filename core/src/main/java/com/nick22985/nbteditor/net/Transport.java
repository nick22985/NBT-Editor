package com.nick22985.nbteditor.net;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface Transport {
	int MAX_SERVERBOUND_FRAME = 30000;
	int MAX_CLIENTBOUND_FRAME = 1000000;

	boolean serverHasChannel();

	boolean clientHasChannel(UUID player);

	void sendToServer(byte[] frame);

	void sendToClient(UUID player, byte[] frame);

	void onServerFrame(BiConsumer<UUID, byte[]> handler);

	void onClientFrame(Consumer<byte[]> handler);
}
