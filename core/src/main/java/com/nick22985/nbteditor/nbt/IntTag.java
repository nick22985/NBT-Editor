package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class IntTag extends NumericTag {
	private final int value;

	private IntTag(int value) {
		this.value = value;
	}

	public static IntTag of(int value) {
		return new IntTag(value);
	}

	public int value() {
		return value;
	}

	@Override
	public TagType type() {
		return TagType.INT;
	}

	@Override
	public Number number() {
		return value;
	}
}
