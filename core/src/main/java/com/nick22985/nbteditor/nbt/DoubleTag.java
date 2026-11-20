package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class DoubleTag extends NumericTag {
	private final double value;

	private DoubleTag(double value) {
		this.value = value;
	}

	public static DoubleTag of(double value) {
		return new DoubleTag(value);
	}

	public double value() {
		return value;
	}

	@Override
	public TagType type() {
		return TagType.DOUBLE;
	}

	@Override
	public Number number() {
		return value;
	}
}
