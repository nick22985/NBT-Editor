package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.PlayerService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PlayerInventoryUtil {
	public static final String INVENTORY = "Inventory";
	public static final String EQUIPMENT = "equipment";
	public static final String ENDER_ITEMS = "EnderItems";
	public static final int ENDER_SIZE = 27;
	private static final List<String> EQUIPMENT_KEYS = List.of("feet", "legs", "chest", "head", "offhand");

	private PlayerInventoryUtil() {
	}

	public static Map<Integer, CompoundTag> slots(CompoundTag player) {
		Map<Integer, CompoundTag> slots = new LinkedHashMap<>();
		ListTag inventory = player.getList(INVENTORY);
		if (inventory != null) {
			for (Tag tag : inventory) {
				if (!(tag instanceof CompoundTag)) {
					continue;
				}
				CompoundTag item = ((CompoundTag) tag).copy();
				Tag slotTag = item.remove("Slot");
				int slot = slotTag instanceof NumericTag ? playerSlot(((NumericTag) slotTag).intValue()) : -1;
				if (slot >= 0) {
					slots.put(slot, item);
				}
			}
		}
		CompoundTag equipment = player.getCompound(EQUIPMENT);
		if (equipment != null) {
			for (int i = 0; i < EQUIPMENT_KEYS.size(); i++) {
				CompoundTag item = equipment.getCompound(EQUIPMENT_KEYS.get(i));
				if (item != null) {
					slots.put(PlayerService.FEET_SLOT + i, item.copy());
				}
			}
		}
		return slots;
	}

	public static Map<Integer, CompoundTag> enderSlots(CompoundTag player) {
		Map<Integer, CompoundTag> slots = new LinkedHashMap<>();
		ListTag items = player.getList(ENDER_ITEMS);
		if (items == null) {
			return slots;
		}
		for (Tag tag : items) {
			if (!(tag instanceof CompoundTag)) {
				continue;
			}
			CompoundTag item = ((CompoundTag) tag).copy();
			Tag slotTag = item.remove("Slot");
			int slot = slotTag instanceof NumericTag ? ((NumericTag) slotTag).intValue() : -1;
			if (slot >= 0 && slot < ENDER_SIZE) {
				slots.put(slot, item);
			}
		}
		return slots;
	}

	public static int playerSlot(int stored) {
		if (stored >= 0 && stored < PlayerService.FEET_SLOT) {
			return stored;
		}
		if (stored >= 100 && stored <= 103) {
			return PlayerService.FEET_SLOT + stored - 100;
		}
		if (stored == -106) {
			return PlayerService.OFFHAND_SLOT;
		}
		return -1;
	}

	public static CompoundTag withoutInventory(CompoundTag player) {
		CompoundTag copy = player.copy();
		copy.remove(INVENTORY);
		copy.remove(EQUIPMENT);
		copy.remove(ENDER_ITEMS);
		return copy;
	}
}
