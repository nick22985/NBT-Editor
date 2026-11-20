package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ByteTag extends NumericTag {
	private final byte value;

	private ByteTag(byte value) {
		this.value = value;
	}

	private static final ByteTag[] CACHE = new ByteTag[256];

	static {
		for (int i = 0; i < 256; i++) {
			CACHE[i] = new ByteTag((byte) (i - 128));
		}
	}

	public static final ByteTag ZERO = of((byte) 0);
	public static final ByteTag ONE = of((byte) 1);

	public static ByteTag of(byte value) {
		return CACHE[value + 128];
	}

	public static ByteTag of(boolean value) {
		return value ? ONE : ZERO;
	}

	public boolean asBoolean() {
		return value != 0;
	}

	public byte value() {
		return value;
	}

	@Override
	public TagType type() {
		return TagType.BYTE;
	}

	@Override
	public Number number() {
		return value;
	}
}
