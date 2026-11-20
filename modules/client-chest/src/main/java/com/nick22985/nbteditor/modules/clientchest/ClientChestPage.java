package com.nick22985.nbteditor.modules.clientchest;

import com.nick22985.nbteditor.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ClientChestPage(int index, List<CompoundTag> items, int dataVersion, PageState state) {
	public static ClientChestPage empty(int index, int dataVersion) {
		List<CompoundTag> items = new ArrayList<>();
		for (int i = 0; i < ClientChestStore.SIZE; i++) {
			items.add(new CompoundTag());
		}
		return new ClientChestPage(index, List.copyOf(items), dataVersion, PageState.OK);
	}

	public boolean isEmpty() {
		return this.count() == 0;
	}

	public int count() {
		int count = 0;
		for (CompoundTag item : this.items) {
			if (!item.isEmpty()) {
				count++;
			}
		}
		return count;
	}

	public CompoundTag item(int slot) {
		return slot >= 0 && slot < this.items.size() ? this.items.get(slot).copy() : new CompoundTag();
	}
}
