package com.nick22985.nbteditor.net;

import com.nick22985.nbteditor.log.Log;
import com.nick22985.nbteditor.util.NetFrameUtil;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NetService {
	private final Transport transport;
	private final Log log;
	private final Map<String, PacketType<?>> types = new ConcurrentHashMap<>();
	private final Map<String, ServerHandler<?>> serverHandlers = new ConcurrentHashMap<>();
	private final Map<String, ClientHandler<?>> clientHandlers = new ConcurrentHashMap<>();
	private final Map<UUID, FrameAssembler> serverAssemblers = new ConcurrentHashMap<>();
	private final FrameAssembler clientAssembler = new FrameAssembler();
	private final AtomicInteger nextMessageId = new AtomicInteger();

	public NetService(Transport transport, Log log) {
		this.transport = transport;
		this.log = log;
		transport.onServerFrame(this::receiveOnServer);
		transport.onClientFrame(this::receiveOnClient);
	}

	public <T> void registerServer(PacketType<T> type, ServerHandler<T> handler) {
		this.register(type);
		this.serverHandlers.put(type.id(), handler);
	}

	public <T> void registerClient(PacketType<T> type, ClientHandler<T> handler) {
		this.register(type);
		this.clientHandlers.put(type.id(), handler);
	}

	public void register(PacketType<?> type) {
		PacketType<?> existing = this.types.putIfAbsent(type.id(), type);
		if (existing != null && existing != type) {
			throw new IllegalStateException("duplicate packet type " + type.id());
		}
	}

	public boolean serverAvailable() {
		return this.transport.serverHasChannel();
	}

	public boolean clientAvailable(UUID player) {
		return this.transport.clientHasChannel(player);
	}

	public <T> void sendToServer(PacketType<T> type, T message) {
		for (byte[] frame : this.encode(type, message, Transport.MAX_SERVERBOUND_FRAME)) {
			this.transport.sendToServer(frame);
		}
	}

	public <T> void sendToClient(UUID player, PacketType<T> type, T message) {
		for (byte[] frame : this.encode(type, message, Transport.MAX_CLIENTBOUND_FRAME)) {
			this.transport.sendToClient(player, frame);
		}
	}

	private <T> List<byte[]> encode(PacketType<T> type, T message, int frameSize) {
		ByteWriter out = new ByteWriter();
		type.codec().write(out, message);
		return NetFrameUtil.split(this.nextMessageId.getAndIncrement() & 0x7FFFFFFF, type.id(), out.toByteArray(), frameSize);
	}

	public void forget(UUID player) {
		this.serverAssemblers.remove(player);
	}

	public void resetClient() {
		this.clientAssembler.clear();
	}

	@SuppressWarnings("unchecked")
	private void receiveOnServer(UUID sender, byte[] data) {
		try {
			AssembledMessage message = this.serverAssemblers.computeIfAbsent(sender, ignored -> new FrameAssembler()).accept(Frame.decode(data));
			if (message == null) {
				return;
			}
			PacketType<Object> type = (PacketType<Object>) this.types.get(message.type());
			ServerHandler<Object> handler = (ServerHandler<Object>) this.serverHandlers.get(message.type());
			if (type == null || handler == null || type.direction() != Direction.SERVERBOUND) {
				this.log.warn("Unknown serverbound packet " + message.type() + " from " + sender);
				return;
			}
			handler.handle(sender, type.codec().read(new ByteReader(message.payload())));
		} catch (RuntimeException e) {
			this.log.warn("Bad packet from " + sender, e);
		}
	}

	@SuppressWarnings("unchecked")
	private void receiveOnClient(byte[] data) {
		try {
			AssembledMessage message = this.clientAssembler.accept(Frame.decode(data));
			if (message == null) {
				return;
			}
			PacketType<Object> type = (PacketType<Object>) this.types.get(message.type());
			ClientHandler<Object> handler = (ClientHandler<Object>) this.clientHandlers.get(message.type());
			if (type == null || handler == null || type.direction() != Direction.CLIENTBOUND) {
				this.log.warn("Unknown clientbound packet " + message.type());
				return;
			}
			handler.handle(type.codec().read(new ByteReader(message.payload())));
		} catch (RuntimeException e) {
			this.log.warn("Bad packet from server", e);
		}
	}
}
