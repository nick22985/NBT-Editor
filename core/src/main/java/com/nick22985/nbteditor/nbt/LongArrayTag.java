package com.nick22985.nbteditor.nbt;

import java.util.Arrays;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class LongArrayTag implements Tag {
	private final long[] value;

	public LongArrayTag(long[] value) {
		this.value = value.clone();
	}

	public long[] value() {
		return value.clone();
	}

	public int size() {
		return value.length;
	}

	public long get(int index) {
		return value[index];
	}

	public LongArrayTag with(int index, long element) {
		long[] next = value.clone();
		next[index] = element;
		return new LongArrayTag(next);
	}

	public LongArrayTag inserted(int index, long element) {
		long[] next = new long[value.length + 1];
		System.arraycopy(value, 0, next, 0, index);
		next[index] = element;
		System.arraycopy(value, index, next, index + 1, value.length - index);
		return new LongArrayTag(next);
	}

	public LongArrayTag removed(int index) {
		long[] next = new long[value.length - 1];
		System.arraycopy(value, 0, next, 0, index);
		System.arraycopy(value, index + 1, next, index, value.length - index - 1);
		return new LongArrayTag(next);
	}

	@Override
	public TagType type() {
		return TagType.LONG_ARRAY;
	}

	@Override
	public LongArrayTag copy() {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof LongArrayTag && Arrays.equals(((LongArrayTag) o).value, value);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(value);
	}

	@Override
	public String toString() {
		return toSnbt();
	}
}
