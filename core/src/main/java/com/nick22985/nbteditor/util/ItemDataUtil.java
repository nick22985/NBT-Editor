package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.FieldValue;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextDialect;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ItemDataUtil {
	public static final String CUSTOM_DATA = "custom_data";

	private ItemDataUtil() {
	}

	public static FieldValue<Text> readText(Tag tag, GameVersion version) {
		if (tag == null) {
			return FieldValue.absent();
		}
		return FieldValue.of(TextCodecUtil.fromStored(tag, TextDialect.of(version)));
	}

	public static List<String> attributeSlots(GameVersion version) {
		List<String> slots = new ArrayList<>(List.of("any", "mainhand", "offhand"));
		if (Feature.ITEM_COMPONENTS.in(version)) {
			slots.add("hand");
			slots.add("armor");
		}
		slots.addAll(List.of("head", "chest", "legs", "feet"));
		if (Feature.BODY_ARMOR_ITEM.in(version)) {
			slots.add("body");
		}
		if (Feature.TOOLTIP_DISPLAY.in(version)) {
			slots.add("saddle");
		}
		return slots;
	}

	public static int maxLevel(GameVersion version) {
		return Feature.ITEM_COMPONENTS.in(version) ? 255 : Short.MAX_VALUE;
	}

	public static Tag dataTag(CompoundTag item, GameVersion version, String key) {
		CompoundTag data = ItemStackUtil.data(item, version);
		return data == null ? null : data.get(key);
	}

	public static void putData(CompoundTag item, GameVersion version, String key, Tag value) {
		if (value == null) {
			CompoundTag data = ItemStackUtil.data(item, version);
			if (data != null) {
				data.remove(key);
				ItemStackUtil.pruneData(item, version);
			}
		} else {
			ItemStackUtil.dataOrCreate(item, version).put(key, value);
		}
	}

	public static CompoundTag display(CompoundTag item, boolean create) {
		CompoundTag data = item.getCompound("tag");
		if (data == null) {
			if (!create) {
				return null;
			}
			data = item.getOrCreateCompound("tag");
		}
		return create ? data.getOrCreateCompound("display") : data.getCompound("display");
	}

	public static void putDisplay(CompoundTag item, GameVersion version, String key, Tag value) {
		if (value == null) {
			CompoundTag display = display(item, false);
			if (display != null) {
				display.remove(key);
				if (display.isEmpty()) {
					item.getCompound("tag").remove("display");
				}
				ItemStackUtil.pruneData(item, version);
			}
		} else {
			display(item, true).put(key, value);
		}
	}

	public static Tag customData(CompoundTag item, GameVersion version, String key) {
		if (!Feature.ITEM_COMPONENTS.in(version)) {
			return dataTag(item, version, key);
		}
		Tag data = ItemStackUtil.component(item, CUSTOM_DATA);
		return data instanceof CompoundTag ? ((CompoundTag) data).get(key) : null;
	}

	public static void putCustomData(CompoundTag item, GameVersion version, String key, Tag value) {
		if (!Feature.ITEM_COMPONENTS.in(version)) {
			putData(item, version, key, value);
			return;
		}
		Tag existing = ItemStackUtil.component(item, CUSTOM_DATA);
		CompoundTag data = existing instanceof CompoundTag ? (CompoundTag) existing.copy() : new CompoundTag();
		if (value == null) {
			data.remove(key);
		} else {
			data.put(key, value);
		}
		ItemStackUtil.setComponent(item, CUSTOM_DATA, data.isEmpty() ? null : data);
	}
}
