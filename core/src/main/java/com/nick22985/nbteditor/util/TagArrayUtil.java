package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.ByteArrayTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.IntArrayTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.LongArrayTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.Tag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TagArrayUtil {
	private TagArrayUtil() {
	}

	public static int containerSize(Tag tag) {
		if (tag instanceof CompoundTag) {
			return ((CompoundTag) tag).size();
		}
		if (tag instanceof ListTag) {
			return ((ListTag) tag).size();
		}
		if (tag instanceof ByteArrayTag) {
			return ((ByteArrayTag) tag).size();
		}
		if (tag instanceof IntArrayTag) {
			return ((IntArrayTag) tag).size();
		}
		if (tag instanceof LongArrayTag) {
			return ((LongArrayTag) tag).size();
		}
		return 0;
	}

	private static NumericTag number(Tag value) {
		if (!(value instanceof NumericTag)) {
			throw new IllegalArgumentException("array_needs_number");
		}
		return (NumericTag) value;
	}

	public static Tag arrayWith(Tag array, int index, Tag value) {
		if (array instanceof ByteArrayTag) {
			return ((ByteArrayTag) array).with(index, number(value).byteValue());
		}
		if (array instanceof IntArrayTag) {
			return ((IntArrayTag) array).with(index, number(value).intValue());
		}
		return ((LongArrayTag) array).with(index, number(value).longValue());
	}

	public static Tag arrayInserted(Tag array, int index, Tag value) {
		if (array instanceof ByteArrayTag) {
			return ((ByteArrayTag) array).inserted(index, number(value).byteValue());
		}
		if (array instanceof IntArrayTag) {
			return ((IntArrayTag) array).inserted(index, number(value).intValue());
		}
		return ((LongArrayTag) array).inserted(index, number(value).longValue());
	}

	public static Tag arrayRemoved(Tag array, int index) {
		if (array instanceof ByteArrayTag) {
			return ((ByteArrayTag) array).removed(index);
		}
		if (array instanceof IntArrayTag) {
			return ((IntArrayTag) array).removed(index);
		}
		return ((LongArrayTag) array).removed(index);
	}
}
