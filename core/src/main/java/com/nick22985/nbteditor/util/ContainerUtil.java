package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.data.ContainerSlot;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.nbt.ByteTag;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.Tag;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ContainerUtil {
	private ContainerUtil() {
	}

	public static List<ContainerSlot> readList(ListTag list, GameVersion version) {
		List<ContainerSlot> slots = new ArrayList<>();
		for (Tag element : list) {
			if (!(element instanceof CompoundTag)) {
				continue;
			}
			CompoundTag entry = (CompoundTag) element;
			if (entry.contains("item")) {
				CompoundTag item = entry.getCompound("item");
				if (item != null) {
					slots.add(new ContainerSlot(entry.getInt("slot", entry.getInt("Slot", slots.size())), item.copy()));
				}
			} else {
				CompoundTag item = entry.copy();
				int slot = item.getInt("Slot", item.getInt("slot", slots.size()));
				item.remove("Slot");
				item.remove("slot");
				slots.add(new ContainerSlot(slot, item));
			}
		}
		return slots;
	}

	public static ListTag writeList(List<ContainerSlot> slots, GameVersion version) {
		if (!Feature.ITEM_COMPONENTS.in(version)) {
			return writeSlotted(slots);
		}
		ListTag list = new ListTag();
		for (ContainerSlot slot : slots) {
			if (ItemStackUtil.isEmpty(slot.item())) {
				continue;
			}
			CompoundTag entry = new CompoundTag();
			entry.putInt("slot", slot.slot());
			entry.put("item", slot.item().copy());
			list.add(entry);
		}
		return list;
	}

	public static ListTag writeSlotted(List<ContainerSlot> slots) {
		ListTag list = new ListTag();
		for (ContainerSlot slot : slots) {
			if (ItemStackUtil.isEmpty(slot.item())) {
				continue;
			}
			CompoundTag entry = new CompoundTag();
			entry.put("Slot", ByteTag.of((byte) slot.slot()));
			for (var e : slot.item().entries()) {
				entry.put(e.getKey(), e.getValue().copy());
			}
			list.add(entry);
		}
		return list;
	}

	public static CompoundTag emptyItem(GameVersion version) {
		return Feature.ITEM_COMPONENTS.in(version) ? new CompoundTag() : ItemStackUtil.create(ItemStackUtil.AIR, 0, version);
	}

	public static void putItem(CompoundTag data, String key, CompoundTag item, boolean itemHost, GameVersion version) {
		if (!ItemStackUtil.isEmpty(item)) {
			data.put(key, item.copy());
		} else if (!itemHost && !Feature.ITEM_COMPONENTS.in(version)) {
			data.put(key, emptyItem(version));
		} else {
			data.remove(key);
		}
	}

	public static CompoundTag readItem(CompoundTag data, String key) {
		CompoundTag item = data.getCompound(key);
		return item == null ? new CompoundTag() : item.copy();
	}

	public static CompoundTag pruneEmpty(CompoundTag data) {
		for (String key : new ArrayList<>(data.keys())) {
			Tag value = data.get(key);
			if (value instanceof ListTag && ((ListTag) value).isEmpty() || value instanceof CompoundTag && ((CompoundTag) value).isEmpty()) {
				data.remove(key);
			}
		}
		return data;
	}

	public static boolean hostEmpty(CompoundTag data) {
		return data.isEmpty() || data.size() == 1 && data.contains("id");
	}

	public static void replace(CompoundTag target, CompoundTag source) {
		for (String key : new ArrayList<>(target.keys())) {
			if (!source.contains(key)) {
				target.remove(key);
			}
		}
		for (var entry : source.entries()) {
			target.put(entry.getKey(), entry.getValue());
		}
	}
}
