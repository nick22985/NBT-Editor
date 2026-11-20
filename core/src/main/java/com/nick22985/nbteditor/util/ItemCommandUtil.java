package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.Tag;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ItemCommandUtil {
	private ItemCommandUtil() {
	}

	public static String argument(CompoundTag item, GameVersion version, SnbtDialect dialect) {
		StringBuilder out = new StringBuilder(ItemStackUtil.id(item));
		CompoundTag data = ItemStackUtil.data(item, version);
		if (Feature.ITEM_COMPONENTS.in(version)) {
			if (data != null && !data.isEmpty()) {
				out.append('[');
				boolean first = true;
				for (Map.Entry<String, Tag> entry : data.entries()) {
					if (!first) {
						out.append(',');
					}
					first = false;
					if (entry.getKey().startsWith("!")) {
						out.append('!').append(ItemStackUtil.namespaced(entry.getKey().substring(1)));
					} else {
						out.append(ItemStackUtil.namespaced(entry.getKey())).append('=').append(DataCommandUtil.snbt(entry.getValue(), dialect));
					}
				}
				out.append(']');
			}
		} else if (data != null && !data.isEmpty()) {
			out.append(DataCommandUtil.snbt(data, dialect));
		}
		return out.toString();
	}

	public static String slotName(int slot) {
		if (slot >= 0 && slot < 9) {
			return "hotbar." + slot;
		}
		if (slot >= 9 && slot < 36) {
			return "inventory." + (slot - 9);
		}
		switch (slot) {
			case 36:
				return "armor.feet";
			case 37:
				return "armor.legs";
			case 38:
				return "armor.chest";
			case 39:
				return "armor.head";
			case 40:
				return "weapon.offhand";
			default:
				return null;
		}
	}

	public static String replaceCommand(String selector, int slot, CompoundTag item, GameVersion version, SnbtDialect dialect) {
		String name = slotName(slot);
		if (name == null) {
			throw new IllegalArgumentException("slot");
		}
		return replaceCommand(selector, name, item, version, dialect);
	}

	public static String replaceCommand(String selector, String name, CompoundTag item, GameVersion version, SnbtDialect dialect) {
		if (ItemStackUtil.isEmpty(item)) {
			return "item replace entity " + selector + " " + name + " with minecraft:air";
		}
		return "item replace entity " + selector + " " + name + " with " + argument(item, version, dialect) + " " + ItemStackUtil.count(item);
	}
}
