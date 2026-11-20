package com.nick22985.nbteditor.net;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
final class LoopbackTransport implements Transport {
	static final UUID PLAYER = new UUID(1, 2);

	final List<byte[]> serverbound = new ArrayList<>();
	private BiConsumer<UUID, byte[]> serverHandler;
	private Consumer<byte[]> clientHandler;

	@Override
	public boolean serverHasChannel() {
		return true;
	}

	@Override
	public boolean clientHasChannel(UUID player) {
		return true;
	}

	@Override
	public void sendToServer(byte[] frame) {
		this.serverbound.add(frame);
		this.serverHandler.accept(PLAYER, frame);
	}

	@Override
	public void sendToClient(UUID player, byte[] frame) {
		this.clientHandler.accept(frame);
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
