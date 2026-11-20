package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ShortTag extends NumericTag {
	private final short value;

	private ShortTag(short value) {
		this.value = value;
	}

	public static ShortTag of(short value) {
		return new ShortTag(value);
	}

	public short value() {
		return value;
	}

	@Override
	public TagType type() {
		return TagType.SHORT;
	}

	@Override
	public Number number() {
		return value;
	}
}
