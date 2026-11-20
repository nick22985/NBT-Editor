package com.nick22985.nbteditor.modules.bridge;

import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.net.ByteReader;
import com.nick22985.nbteditor.net.ByteWriter;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.BlockWriteFlags;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record BridgeRequest(int id, BridgeOp op, String dimension, BlockPos pos, UUID uuid, int slot, String key, String value, Tag data, BlockWriteFlags flags) {
	public static BridgeRequest of(int id, BridgeOp op) {
		return new BridgeRequest(id, op, null, null, null, -1, null, null, null, null);
	}

	public BridgeRequest at(String dimension, BlockPos pos) {
		return new BridgeRequest(this.id, this.op, dimension, pos, this.uuid, this.slot, this.key, this.value, this.data, this.flags);
	}

	public BridgeRequest in(String dimension) {
		return new BridgeRequest(this.id, this.op, dimension, this.pos, this.uuid, this.slot, this.key, this.value, this.data, this.flags);
	}

	public BridgeRequest subject(UUID uuid) {
		return new BridgeRequest(this.id, this.op, this.dimension, this.pos, uuid, this.slot, this.key, this.value, this.data, this.flags);
	}

	public BridgeRequest slot(int slot) {
		return new BridgeRequest(this.id, this.op, this.dimension, this.pos, this.uuid, slot, this.key, this.value, this.data, this.flags);
	}

	public BridgeRequest entry(String key, String value) {
		return new BridgeRequest(this.id, this.op, this.dimension, this.pos, this.uuid, this.slot, key, value, this.data, this.flags);
	}

	public BridgeRequest data(Tag data) {
		return new BridgeRequest(this.id, this.op, this.dimension, this.pos, this.uuid, this.slot, this.key, this.value, data, this.flags);
	}

	public BridgeRequest flags(BlockWriteFlags flags) {
		return new BridgeRequest(this.id, this.op, this.dimension, this.pos, this.uuid, this.slot, this.key, this.value, this.data, flags);
	}

	public void write(ByteWriter out) {
		out.writeVarInt(this.id);
		out.writeEnum(this.op);
		out.writeNullableString(this.dimension);
		out.writeBoolean(this.pos != null);
		if (this.pos != null) {
			out.writeInt(this.pos.x()).writeInt(this.pos.y()).writeInt(this.pos.z());
		}
		out.writeBoolean(this.uuid != null);
		if (this.uuid != null) {
			out.writeUuid(this.uuid);
		}
		out.writeVarInt(this.slot + 1);
		out.writeNullableString(this.key);
		out.writeNullableString(this.value);
		out.writeNullableTag(this.data);
		out.writeBoolean(this.flags != null);
		if (this.flags != null) {
			out.writeBoolean(this.flags.updateNeighbors()).writeBoolean(this.flags.notifyClients()).writeBoolean(this.flags.keepState()).writeBoolean(this.flags.triggerUpdates()).writeBoolean(this.flags.recreate());
		}
	}

	public static BridgeRequest read(ByteReader in) {
		int id = in.readVarInt();
		BridgeOp op = in.readEnum(BridgeOp.class);
		String dimension = in.readNullableString();
		BlockPos pos = in.readBoolean() ? new BlockPos(in.readInt(), in.readInt(), in.readInt()) : null;
		UUID uuid = in.readBoolean() ? in.readUuid() : null;
		int slot = in.readVarInt() - 1;
		String key = in.readNullableString();
		String value = in.readNullableString();
		Tag data = in.readNullableTag();
		BlockWriteFlags flags = in.readBoolean() ? new BlockWriteFlags(in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean()) : null;
		return new BridgeRequest(id, op, dimension, pos, uuid, slot, key, value, data, flags);
	}
}
