package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class LongTag extends NumericTag {
	private final long value;

	private LongTag(long value) {
		this.value = value;
	}

	public static LongTag of(long value) {
		return new LongTag(value);
	}

	public long value() {
		return value;
	}

	@Override
	public TagType type() {
		return TagType.LONG;
	}

	@Override
	public Number number() {
		return value;
	}
}
