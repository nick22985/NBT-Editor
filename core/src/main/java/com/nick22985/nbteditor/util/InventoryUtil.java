package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.ItemService;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class InventoryUtil {
	private InventoryUtil() {
	}

	public static boolean present(GameItem item) {
		return item != null && !item.isEmpty();
	}

	public static boolean stackable(GameItem a, GameItem b) {
		if (!present(a) || !present(b) || !a.id().equals(b.id())) {
			return false;
		}
		return withoutCount(a.tag()).equals(withoutCount(b.tag()));
	}

	public static int limit(ItemService items, GameItem slot, GameItem incoming) {
		int max = items.maxStackSize(incoming);
		return present(slot) ? Math.max(max, slot.count()) : max;
	}

	public static GameItem withCount(ItemService items, GameItem item, int count) {
		return count <= 0 || !present(item) ? items.empty() : item.withCount(count);
	}

	public static CompoundTag withoutCount(CompoundTag tag) {
		CompoundTag copy = tag.copy();
		copy.remove("count");
		copy.remove("Count");
		return copy;
	}
}
