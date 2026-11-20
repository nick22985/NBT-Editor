package com.nick22985.nbteditor.modules.bridge;

import com.nick22985.nbteditor.net.ByteReader;
import com.nick22985.nbteditor.net.ByteWriter;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record BridgeWelcome(int protocol, int permission, int required, boolean otherPlayers) {
	public void write(ByteWriter out) {
		out.writeVarInt(this.protocol).writeVarInt(this.permission).writeVarInt(this.required).writeBoolean(this.otherPlayers);
	}

	public static BridgeWelcome read(ByteReader in) {
		return new BridgeWelcome(in.readVarInt(), in.readVarInt(), in.readVarInt(), in.readBoolean());
	}

	public boolean allowed() {
		return this.protocol == BridgePackets.PROTOCOL && this.permission >= this.required;
	}
}
