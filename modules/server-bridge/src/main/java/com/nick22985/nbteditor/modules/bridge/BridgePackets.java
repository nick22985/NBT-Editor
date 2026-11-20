package com.nick22985.nbteditor.modules.bridge;

import com.nick22985.nbteditor.net.ByteReader;
import com.nick22985.nbteditor.net.ByteWriter;
import com.nick22985.nbteditor.net.MessageCodec;
import com.nick22985.nbteditor.net.PacketType;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BridgePackets {
	public static final int PROTOCOL = 1;
	public static final PacketType<Integer> HELLO = PacketType.serverbound("bridge/hello", MessageCodec.of((ByteWriter out, Integer protocol) -> out.writeVarInt(protocol), ByteReader::readVarInt));
	public static final PacketType<BridgeWelcome> WELCOME = PacketType.clientbound("bridge/welcome", MessageCodec.of((ByteWriter out, BridgeWelcome message) -> message.write(out), BridgeWelcome::read));
	public static final PacketType<BridgeRequest> REQUEST = PacketType.serverbound("bridge/request", MessageCodec.of((ByteWriter out, BridgeRequest message) -> message.write(out), BridgeRequest::read));
	public static final PacketType<BridgeResponse> RESPONSE = PacketType.clientbound("bridge/response", MessageCodec.of((ByteWriter out, BridgeResponse message) -> message.write(out), BridgeResponse::read));

	private BridgePackets() {
	}
}
