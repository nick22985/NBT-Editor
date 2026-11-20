package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BundleStore implements SlotStore {
	private final ContainerLayout layout = ContainerLayout.grid(9, 3);

	@Override
	public ContainerLayout layout(StoreContext context) {
		return this.layout;
	}

	private List<CompoundTag> entries(StoreContext context) {
		List<CompoundTag> entries = ItemFields.BUNDLE.get(context.subject().root(), context.version());
		return entries == null ? List.of() : entries;
	}

	@Override
	public List<CompoundTag> read(StoreContext context) {
		List<CompoundTag> entries = this.entries(context);
		List<CompoundTag> items = new ArrayList<>();
		for (int i = 0; i < this.layout.size(); i++) {
			items.add(i < entries.size() ? entries.get(i).copy() : new CompoundTag());
		}
		return items;
	}

	@Override
	public void write(StoreContext context, List<CompoundTag> items) {
		List<CompoundTag> entries = this.entries(context);
		List<CompoundTag> next = new ArrayList<>();
		for (int i = 0; i < this.layout.size() && i < items.size(); i++) {
			if (!ItemStackUtil.isEmpty(items.get(i))) {
				next.add(items.get(i).copy());
			}
		}
		for (int i = this.layout.size(); i < entries.size(); i++) {
			next.add(entries.get(i).copy());
		}
		ItemFields.BUNDLE.write(context.subject().root(), context.version(), next.isEmpty() ? null : next);
	}
}
