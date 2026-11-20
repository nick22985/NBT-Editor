package com.nick22985.nbteditor.net;

import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.NbtIoUtil;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ByteWriter {
	private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	private final DataOutputStream out = new DataOutputStream(this.bytes);

	public ByteWriter writeByte(int value) {
		this.bytes.write(value);
		return this;
	}

	public ByteWriter writeBoolean(boolean value) {
		return this.writeByte(value ? 1 : 0);
	}

	public ByteWriter writeVarInt(int value) {
		int remaining = value;
		while ((remaining & ~0x7F) != 0) {
			this.bytes.write(remaining & 0x7F | 0x80);
			remaining >>>= 7;
		}
		this.bytes.write(remaining);
		return this;
	}

	public ByteWriter writeVarLong(long value) {
		long remaining = value;
		while ((remaining & ~0x7FL) != 0) {
			this.bytes.write((int) (remaining & 0x7F) | 0x80);
			remaining >>>= 7;
		}
		this.bytes.write((int) remaining);
		return this;
	}

	public ByteWriter writeInt(int value) {
		try {
			this.out.writeInt(value);
		} catch (IOException e) {
			throw new NetException("write failed", e);
		}
		return this;
	}

	public ByteWriter writeLong(long value) {
		try {
			this.out.writeLong(value);
		} catch (IOException e) {
			throw new NetException("write failed", e);
		}
		return this;
	}

	public ByteWriter writeDouble(double value) {
		return this.writeLong(Double.doubleToRawLongBits(value));
	}

	public ByteWriter writeFloat(float value) {
		return this.writeInt(Float.floatToRawIntBits(value));
	}

	public ByteWriter writeBytes(byte[] value) {
		this.writeVarInt(value.length);
		this.bytes.write(value, 0, value.length);
		return this;
	}

	public ByteWriter writeRaw(byte[] value, int offset, int length) {
		this.bytes.write(value, offset, length);
		return this;
	}

	public ByteWriter writeString(String value) {
		return this.writeBytes(value.getBytes(StandardCharsets.UTF_8));
	}

	public ByteWriter writeNullableString(String value) {
		this.writeBoolean(value != null);
		if (value != null) {
			this.writeString(value);
		}
		return this;
	}

	public ByteWriter writeUuid(UUID value) {
		return this.writeLong(value.getMostSignificantBits()).writeLong(value.getLeastSignificantBits());
	}

	public ByteWriter writeTag(Tag tag) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			NbtIoUtil.writeUnnamed(tag, new DataOutputStream(buffer));
		} catch (IOException e) {
			throw new NetException("tag write failed", e);
		}
		return this.writeBytes(buffer.toByteArray());
	}

	public ByteWriter writeNullableTag(Tag tag) {
		this.writeBoolean(tag != null);
		if (tag != null) {
			this.writeTag(tag);
		}
		return this;
	}

	public <E extends Enum<E>> ByteWriter writeEnum(E value) {
		return this.writeVarInt(value.ordinal());
	}

	public int size() {
		return this.bytes.size();
	}

	public byte[] toByteArray() {
		return this.bytes.toByteArray();
	}
}
