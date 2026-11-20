package com.nick22985.nbteditor.net;

import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.NbtIoUtil;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ByteReader {
	private static final int MAX_STRING = 1 << 22;

	private final byte[] data;
	private final int end;
	private int position;

	public ByteReader(byte[] data) {
		this(data, 0, data.length);
	}

	public ByteReader(byte[] data, int offset, int length) {
		this.data = data;
		this.position = offset;
		this.end = offset + length;
	}

	public int remaining() {
		return this.end - this.position;
	}

	public int position() {
		return this.position;
	}

	private void require(int count) {
		if (count < 0 || this.remaining() < count) {
			throw new NetException("buffer underflow: need " + count + ", have " + this.remaining());
		}
	}

	public int readByte() {
		this.require(1);
		return this.data[this.position++] & 0xFF;
	}

	public boolean readBoolean() {
		return this.readByte() != 0;
	}

	public int readVarInt() {
		int value = 0;
		int shift = 0;
		int current;
		do {
			if (shift >= 35) {
				throw new NetException("varint too long");
			}
			current = this.readByte();
			value |= (current & 0x7F) << shift;
			shift += 7;
		} while ((current & 0x80) != 0);
		return value;
	}

	public long readVarLong() {
		long value = 0;
		int shift = 0;
		int current;
		do {
			if (shift >= 70) {
				throw new NetException("varlong too long");
			}
			current = this.readByte();
			value |= (long) (current & 0x7F) << shift;
			shift += 7;
		} while ((current & 0x80) != 0);
		return value;
	}

	public int readInt() {
		this.require(4);
		int value = (this.data[this.position] & 0xFF) << 24 | (this.data[this.position + 1] & 0xFF) << 16 | (this.data[this.position + 2] & 0xFF) << 8 | this.data[this.position + 3] & 0xFF;
		this.position += 4;
		return value;
	}

	public long readLong() {
		return (long) this.readInt() << 32 | this.readInt() & 0xFFFFFFFFL;
	}

	public double readDouble() {
		return Double.longBitsToDouble(this.readLong());
	}

	public float readFloat() {
		return Float.intBitsToFloat(this.readInt());
	}

	public byte[] readBytes() {
		int length = this.readVarInt();
		this.require(length);
		byte[] out = Arrays.copyOfRange(this.data, this.position, this.position + length);
		this.position += length;
		return out;
	}

	public byte[] readRemaining() {
		byte[] out = Arrays.copyOfRange(this.data, this.position, this.end);
		this.position = this.end;
		return out;
	}

	public String readString() {
		int length = this.readVarInt();
		if (length > MAX_STRING) {
			throw new NetException("string too long: " + length);
		}
		this.require(length);
		String value = new String(this.data, this.position, length, StandardCharsets.UTF_8);
		this.position += length;
		return value;
	}

	public String readNullableString() {
		return this.readBoolean() ? this.readString() : null;
	}

	public UUID readUuid() {
		return new UUID(this.readLong(), this.readLong());
	}

	public Tag readTag() {
		byte[] bytes = this.readBytes();
		try {
			return NbtIoUtil.readUnnamed(new DataInputStream(new ByteArrayInputStream(bytes)));
		} catch (IOException e) {
			throw new NetException("tag read failed", e);
		}
	}

	public Tag readNullableTag() {
		return this.readBoolean() ? this.readTag() : null;
	}

	public <E extends Enum<E>> E readEnum(Class<E> type) {
		int ordinal = this.readVarInt();
		E[] values = type.getEnumConstants();
		if (ordinal < 0 || ordinal >= values.length) {
			throw new NetException("bad enum ordinal " + ordinal + " for " + type.getSimpleName());
		}
		return values[ordinal];
	}
}
