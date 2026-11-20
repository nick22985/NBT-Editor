package com.nick22985.nbteditor.nbt;

import java.util.Objects;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class StringTag implements Tag {
	public static final StringTag EMPTY = new StringTag("");

	private final String value;

	private StringTag(String value) {
		this.value = Objects.requireNonNull(value);
	}

	public static StringTag of(String value) {
		return value.isEmpty() ? EMPTY : new StringTag(value);
	}

	public String value() {
		return value;
	}

	@Override
	public TagType type() {
		return TagType.STRING;
	}

	@Override
	public StringTag copy() {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof StringTag && ((StringTag) o).value.equals(value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return toSnbt();
	}
}
