package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.ByteArrayTag;
import com.nick22985.nbteditor.nbt.ByteTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.IntArrayTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.LongArrayTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtJsonUtil {
	private NbtJsonUtil() {
	}

	public static Object toJson(Tag tag) {
		if (tag instanceof CompoundTag) {
			Map<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<String, Tag> entry : ((CompoundTag) tag).entries()) {
				out.put(entry.getKey(), toJson(entry.getValue()));
			}
			return out;
		}
		if (tag instanceof ListTag) {
			List<Object> out = new ArrayList<>();
			for (Tag element : (ListTag) tag) {
				out.add(toJson(element));
			}
			return out;
		}
		if (tag instanceof StringTag) {
			return ((StringTag) tag).value();
		}
		if (tag instanceof ByteTag) {
			return (int) ((ByteTag) tag).byteValue();
		}
		if (tag instanceof NumericTag) {
			return ((NumericTag) tag).number();
		}
		if (tag instanceof ByteArrayTag) {
			List<Object> out = new ArrayList<>();
			for (byte value : ((ByteArrayTag) tag).value()) {
				out.add((int) value);
			}
			return out;
		}
		if (tag instanceof IntArrayTag) {
			List<Object> out = new ArrayList<>();
			for (int value : ((IntArrayTag) tag).value()) {
				out.add(value);
			}
			return out;
		}
		if (tag instanceof LongArrayTag) {
			List<Object> out = new ArrayList<>();
			for (long value : ((LongArrayTag) tag).value()) {
				out.add(value);
			}
			return out;
		}
		return null;
	}

	public static String pretty(Tag tag) {
		return JsonUtil.pretty(toJson(tag));
	}
}
