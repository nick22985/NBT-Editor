package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.json.JsonNull;
import com.nick22985.nbteditor.nbt.ByteArrayTag;
import com.nick22985.nbteditor.nbt.ByteTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.DoubleTag;
import com.nick22985.nbteditor.nbt.FloatTag;
import com.nick22985.nbteditor.nbt.IntArrayTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.LongArrayTag;
import com.nick22985.nbteditor.nbt.LongTag;
import com.nick22985.nbteditor.nbt.ShortTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TagTreeUtil {
	private TagTreeUtil() {
	}

	public static Object toTree(Tag tag) {
		switch (tag.type()) {
			case COMPOUND: {
				Map<String, Object> map = new LinkedHashMap<>();
				for (Map.Entry<String, Tag> entry : ((CompoundTag) tag).entries()) {
					map.put(entry.getKey(), toTree(entry.getValue()));
				}
				return map;
			}
			case LIST: {
				List<Object> list = new ArrayList<>();
				for (Tag element : (ListTag) tag) {
					list.add(toTree(element));
				}
				return list;
			}
			case STRING:
				return ((StringTag) tag).value();
			case BYTE_ARRAY: {
				List<Object> list = new ArrayList<>();
				for (byte b : ((ByteArrayTag) tag).value()) {
					list.add((int) b);
				}
				return list;
			}
			case INT_ARRAY: {
				List<Object> list = new ArrayList<>();
				for (int i : ((IntArrayTag) tag).value()) {
					list.add(i);
				}
				return list;
			}
			case LONG_ARRAY: {
				List<Object> list = new ArrayList<>();
				for (long l : ((LongArrayTag) tag).value()) {
					list.add(l);
				}
				return list;
			}
			case BYTE:
				return (int) ((ByteTag) tag).value();
			case SHORT:
				return (int) ((ShortTag) tag).value();
			case INT:
				return ((IntTag) tag).value();
			case LONG:
				return ((LongTag) tag).value();
			case FLOAT:
				return (double) ((FloatTag) tag).value();
			case DOUBLE:
				return ((DoubleTag) tag).value();
			default:
				return JsonNull.INSTANCE;
		}
	}

	@SuppressWarnings("unchecked")
	public static Tag fromTree(Object value) {
		if (value instanceof Map) {
			CompoundTag compound = new CompoundTag();
			for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
				if (entry.getValue() != null && entry.getValue() != JsonNull.INSTANCE) {
					compound.put(entry.getKey(), fromTree(entry.getValue()));
				}
			}
			return compound;
		}
		if (value instanceof List) {
			ListTag list = new ListTag();
			for (Object element : (List<Object>) value) {
				if (element != null && element != JsonNull.INSTANCE) {
					list.add(fromTree(element));
				}
			}
			return list;
		}
		if (value instanceof String) {
			return StringTag.of((String) value);
		}
		if (value instanceof Boolean) {
			return ByteTag.of((Boolean) value);
		}
		if (value instanceof Byte) {
			return ByteTag.of((Byte) value);
		}
		if (value instanceof Short) {
			return ShortTag.of((Short) value);
		}
		if (value instanceof Integer) {
			return IntTag.of((Integer) value);
		}
		if (value instanceof Long) {
			return LongTag.of((Long) value);
		}
		if (value instanceof Float) {
			return FloatTag.of((Float) value);
		}
		if (value instanceof Number) {
			return DoubleTag.of(((Number) value).doubleValue());
		}
		if (value instanceof Tag) {
			return ((Tag) value).copy();
		}
		return StringTag.of(String.valueOf(value));
	}
}
