package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.ByteTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.Tag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ItemStackUtil {
	public static final String AIR = "minecraft:air";

	private ItemStackUtil() {
	}

	public static CompoundTag create(String id, int count, GameVersion version) {
		CompoundTag item = new CompoundTag();
		item.putString("id", namespaced(id));
		setCount(item, count, version);
		return item;
	}

	public static String id(CompoundTag item) {
		return namespaced(item.getString("id", AIR));
	}

	public static void setId(CompoundTag item, String id) {
		item.putString("id", namespaced(id));
	}

	public static int count(CompoundTag item) {
		Tag tag = item.get("count");
		if (tag == null) {
			tag = item.get("Count");
		}
		return tag instanceof NumericTag ? ((NumericTag) tag).intValue() : 1;
	}

	public static void setCount(CompoundTag item, int count, GameVersion version) {
		if (Feature.ITEM_COMPONENTS.in(version)) {
			item.remove("Count");
			item.put("count", IntTag.of(count));
		} else {
			item.remove("count");
			item.put("Count", ByteTag.of((byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, count))));
		}
	}

	public static int maxCount(GameVersion version) {
		return Feature.ITEM_COUNT_ABOVE_127.in(version) ? 99 : 64;
	}

	public static boolean isEmpty(CompoundTag item) {
		return item == null || id(item).equals(AIR) || count(item) <= 0;
	}

	public static String dataKey(GameVersion version) {
		return Feature.ITEM_COMPONENTS.in(version) ? "components" : "tag";
	}

	public static CompoundTag data(CompoundTag item, GameVersion version) {
		return item.getCompound(dataKey(version));
	}

	public static CompoundTag dataOrCreate(CompoundTag item, GameVersion version) {
		return item.getOrCreateCompound(dataKey(version));
	}

	public static void pruneData(CompoundTag item, GameVersion version) {
		CompoundTag data = data(item, version);
		if (data != null && data.isEmpty()) {
			item.remove(dataKey(version));
		}
	}

	public static Tag component(CompoundTag item, String key) {
		CompoundTag components = item.getCompound("components");
		if (components == null) {
			return null;
		}
		Tag tag = components.get(namespaced(key));
		if (tag == null) {
			tag = components.get(stripNamespace(key));
		}
		if (tag == null) {
			tag = components.get(":" + stripNamespace(key));
		}
		if (tag == null) {
			tag = components.get("!" + namespaced(key));
		}
		return tag;
	}

	public static boolean isComponentRemoved(CompoundTag item, String key) {
		CompoundTag components = item.getCompound("components");
		return components != null && (components.contains("!" + namespaced(key)) || components.contains("!" + stripNamespace(key)));
	}

	public static void setComponent(CompoundTag item, String key, Tag value) {
		CompoundTag components = item.getOrCreateCompound("components");
		components.remove(stripNamespace(key));
		components.remove(":" + stripNamespace(key));
		components.remove("!" + namespaced(key));
		components.remove("!" + stripNamespace(key));
		if (value == null) {
			components.remove(namespaced(key));
			if (components.isEmpty()) {
				item.remove("components");
			}
		} else {
			components.put(namespaced(key), value);
		}
	}

	public static String namespaced(String id) {
		if (id == null || id.isEmpty()) {
			return AIR;
		}
		if (id.startsWith(":")) {
			return "minecraft" + id;
		}
		return id.indexOf(':') >= 0 ? id : "minecraft:" + id;
	}

	public static String stripNamespace(String id) {
		if (id.startsWith(":")) {
			return id.substring(1);
		}
		return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
	}
}
