package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.ByteArrayTag;
import com.nick22985.nbteditor.nbt.ByteTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.IntArrayTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.LongArrayTag;
import com.nick22985.nbteditor.nbt.LongTag;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.Tag;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtPathUtil {
	private NbtPathUtil() {
	}

	public static Tag step(Tag tag, Object segment) {
		if (segment instanceof String) {
			return tag instanceof CompoundTag ? ((CompoundTag) tag).get((String) segment) : null;
		}
		int index = (Integer) segment;
		if (tag instanceof ListTag) {
			ListTag list = (ListTag) tag;
			return index >= 0 && index < list.size() ? list.get(index) : null;
		}
		if (tag instanceof ByteArrayTag) {
			ByteArrayTag array = (ByteArrayTag) tag;
			return index >= 0 && index < array.size() ? ByteTag.of(array.get(index)) : null;
		}
		if (tag instanceof IntArrayTag) {
			IntArrayTag array = (IntArrayTag) tag;
			return index >= 0 && index < array.size() ? IntTag.of(array.get(index)) : null;
		}
		if (tag instanceof LongArrayTag) {
			LongArrayTag array = (LongArrayTag) tag;
			return index >= 0 && index < array.size() ? LongTag.of(array.get(index)) : null;
		}
		return null;
	}

	public static String formatSegment(Object segment) {
		if (segment instanceof Integer) {
			return "[" + segment + "]";
		}
		String key = (String) segment;
		boolean simple = !key.isEmpty();
		for (int i = 0; i < key.length() && simple; i++) {
			char c = key.charAt(i);
			simple = SnbtUtil.isUnquotedChar(c) && c != '.';
		}
		return simple ? key : "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	public static NbtPath shiftedForInsert(NbtPath path, NbtPath inserted) {
		if (!(inserted.last() instanceof Integer) || path.size() < inserted.size() || !path.startsWith(inserted.parent())) {
			return path;
		}
		int depth = inserted.size() - 1;
		Object segment = path.segments().get(depth);
		if (!(segment instanceof Integer) || (Integer) segment < (Integer) inserted.last()) {
			return path;
		}
		List<Object> segments = new ArrayList<>(path.segments());
		segments.set(depth, (Integer) segment + 1);
		return NbtPath.of(segments.toArray());
	}
}
