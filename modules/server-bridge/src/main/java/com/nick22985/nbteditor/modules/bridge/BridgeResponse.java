package com.nick22985.nbteditor.modules.bridge;

import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.net.ByteReader;
import com.nick22985.nbteditor.net.ByteWriter;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record BridgeResponse(int id, String error, Tag data) {
	public static BridgeResponse ok(int id, Tag data) {
		return new BridgeResponse(id, null, data);
	}

	public static BridgeResponse failed(int id, String error) {
		return new BridgeResponse(id, error, null);
	}

	public boolean succeeded() {
		return this.error == null;
	}

	public void write(ByteWriter out) {
		out.writeVarInt(this.id);
		out.writeNullableString(this.error);
		out.writeNullableTag(this.data);
	}

	public static BridgeResponse read(ByteReader in) {
		return new BridgeResponse(in.readVarInt(), in.readNullableString(), in.readNullableTag());
	}
}
