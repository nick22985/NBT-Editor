package com.nick22985.nbteditor.modules.slotunlock;

import com.nick22985.nbteditor.net.ByteReader;
import com.nick22985.nbteditor.net.ByteWriter;
import com.nick22985.nbteditor.net.MessageCodec;
import com.nick22985.nbteditor.net.PacketType;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SlotUnlockPackets {
	public static final PacketType<Integer> STATE = PacketType.serverbound("slot_unlock/state", MessageCodec.of((ByteWriter out, Integer mask) -> out.writeVarInt(mask), ByteReader::readVarInt));
	public static final PacketType<Boolean> ACK = PacketType.clientbound("slot_unlock/ack", MessageCodec.of((ByteWriter out, Boolean allowed) -> out.writeBoolean(allowed), ByteReader::readBoolean));

	private SlotUnlockPackets() {
	}
}
