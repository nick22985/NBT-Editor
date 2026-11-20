package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public abstract class NumericTag implements Tag {
	public abstract Number number();

	public long longValue() {
		return number().longValue();
	}

	public int intValue() {
		return number().intValue();
	}

	public double doubleValue() {
		return number().doubleValue();
	}

	public float floatValue() {
		return number().floatValue();
	}

	public byte byteValue() {
		return number().byteValue();
	}

	public short shortValue() {
		return number().shortValue();
	}

	@Override
	public NumericTag copy() {
		return this;
	}

	public static NumericTag of(TagType type, Number value) {
		switch (type) {
			case BYTE:
				return ByteTag.of(value.byteValue());
			case SHORT:
				return ShortTag.of(value.shortValue());
			case INT:
				return IntTag.of(value.intValue());
			case LONG:
				return LongTag.of(value.longValue());
			case FLOAT:
				return FloatTag.of(value.floatValue());
			case DOUBLE:
				return DoubleTag.of(value.doubleValue());
			default:
				throw new IllegalArgumentException(type + " is not numeric");
		}
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof NumericTag && ((NumericTag) o).type() == type() && number().equals(((NumericTag) o).number());
	}

	@Override
	public int hashCode() {
		return type().hashCode() * 31 + number().hashCode();
	}

	@Override
	public String toString() {
		return toSnbt();
	}
}
