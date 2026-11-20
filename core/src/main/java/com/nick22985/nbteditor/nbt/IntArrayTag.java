package com.nick22985.nbteditor.nbt;

import java.util.Arrays;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class IntArrayTag implements Tag {
	private final int[] value;

	public IntArrayTag(int[] value) {
		this.value = value.clone();
	}

	public int[] value() {
		return value.clone();
	}

	public int size() {
		return value.length;
	}

	public int get(int index) {
		return value[index];
	}

	public IntArrayTag with(int index, int element) {
		int[] next = value.clone();
		next[index] = element;
		return new IntArrayTag(next);
	}

	public IntArrayTag inserted(int index, int element) {
		int[] next = new int[value.length + 1];
		System.arraycopy(value, 0, next, 0, index);
		next[index] = element;
		System.arraycopy(value, index, next, index + 1, value.length - index);
		return new IntArrayTag(next);
	}

	public IntArrayTag removed(int index) {
		int[] next = new int[value.length - 1];
		System.arraycopy(value, 0, next, 0, index);
		System.arraycopy(value, index + 1, next, index, value.length - index - 1);
		return new IntArrayTag(next);
	}

	@Override
	public TagType type() {
		return TagType.INT_ARRAY;
	}

	@Override
	public IntArrayTag copy() {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof IntArrayTag && Arrays.equals(((IntArrayTag) o).value, value);
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
