package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FloatTag extends NumericTag {
	private final float value;

	private FloatTag(float value) {
		this.value = value;
	}

	public static FloatTag of(float value) {
		return new FloatTag(value);
	}

	public float value() {
		return value;
	}

	@Override
	public TagType type() {
		return TagType.FLOAT;
	}

	@Override
	public Number number() {
		return value;
	}
}
