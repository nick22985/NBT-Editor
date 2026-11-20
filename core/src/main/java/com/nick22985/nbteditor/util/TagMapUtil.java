package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
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
public final class TagMapUtil {
	private TagMapUtil() {
	}

	public static CompoundTag fromStrings(Map<String, String> map) {
		CompoundTag tag = new CompoundTag();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			tag.putString(entry.getKey(), entry.getValue());
		}
		return tag;
	}

	public static Map<String, String> toStrings(CompoundTag tag) {
		Map<String, String> map = new LinkedHashMap<>();
		for (Map.Entry<String, Tag> entry : tag.entries()) {
			String value = scalar(entry.getValue());
			if (value != null) {
				map.put(entry.getKey(), value);
			}
		}
		return map;
	}

	public static List<String> strings(ListTag list) {
		List<String> values = new ArrayList<>();
		for (Tag tag : list) {
			String value = scalar(tag);
			if (value != null) {
				values.add(value);
			}
		}
		return values;
	}

	public static String scalar(Tag tag) {
		if (tag instanceof StringTag) {
			return ((StringTag) tag).value();
		}
		if (tag instanceof NumericTag) {
			return String.valueOf(((NumericTag) tag).number());
		}
		return null;
	}
}
