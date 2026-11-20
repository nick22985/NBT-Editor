package com.nick22985.nbteditor.net;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record Frame(int messageId, int part, int parts, String type, byte[] payload) {
	public byte[] encode() {
		ByteWriter out = new ByteWriter();
		out.writeVarInt(this.messageId).writeVarInt(this.part).writeVarInt(this.parts);
		if (this.part == 0) {
			out.writeString(this.type);
		}
		out.writeRaw(this.payload, 0, this.payload.length);
		return out.toByteArray();
	}

	public static Frame decode(byte[] data) {
		ByteReader in = new ByteReader(data);
		int messageId = in.readVarInt();
		int part = in.readVarInt();
		int parts = in.readVarInt();
		if (parts <= 0 || part < 0 || part >= parts) {
			throw new NetException("bad frame header " + part + "/" + parts);
		}
		String type = part == 0 ? in.readString() : null;
		return new Frame(messageId, part, parts, type, in.readRemaining());
	}
}
