package com.nick22985.nbteditor.nbt;

import java.util.Arrays;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ByteArrayTag implements Tag {
	private final byte[] value;

	public ByteArrayTag(byte[] value) {
		this.value = value.clone();
	}

	public byte[] value() {
		return value.clone();
	}

	public int size() {
		return value.length;
	}

	public byte get(int index) {
		return value[index];
	}

	public ByteArrayTag with(int index, byte element) {
		byte[] next = value.clone();
		next[index] = element;
		return new ByteArrayTag(next);
	}

	public ByteArrayTag inserted(int index, byte element) {
		byte[] next = new byte[value.length + 1];
		System.arraycopy(value, 0, next, 0, index);
		next[index] = element;
		System.arraycopy(value, index, next, index + 1, value.length - index);
		return new ByteArrayTag(next);
	}

	public ByteArrayTag removed(int index) {
		byte[] next = new byte[value.length - 1];
		System.arraycopy(value, 0, next, 0, index);
		System.arraycopy(value, index + 1, next, index, value.length - index - 1);
		return new ByteArrayTag(next);
	}

	@Override
	public TagType type() {
		return TagType.BYTE_ARRAY;
	}

	@Override
	public ByteArrayTag copy() {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ByteArrayTag && Arrays.equals(((ByteArrayTag) o).value, value);
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
