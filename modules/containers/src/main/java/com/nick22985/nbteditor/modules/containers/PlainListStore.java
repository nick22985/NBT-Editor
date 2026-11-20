package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PlainListStore implements SlotStore {
	private final String key;
	private final ContainerLayout layout;

	public PlainListStore(String key, ContainerLayout layout) {
		this.key = key;
		this.layout = layout;
	}

	@Override
	public ContainerLayout layout(StoreContext context) {
		return this.layout;
	}

	private List<CompoundTag> entries(StoreContext context) {
		List<CompoundTag> entries = new ArrayList<>();
		ListTag list = context.data().getList(this.key);
		if (list != null) {
			for (Tag tag : list) {
				if (tag instanceof CompoundTag) {
					entries.add((CompoundTag) tag);
				}
			}
		}
		return entries;
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
		ListTag list = new ListTag();
		for (int i = 0; i < this.layout.size() && i < items.size(); i++) {
			if (!ItemStackUtil.isEmpty(items.get(i))) {
				list.add(items.get(i).copy());
			}
		}
		for (int i = this.layout.size(); i < entries.size(); i++) {
			list.add(entries.get(i).copy());
		}
		CompoundTag data = context.data();
		data.put(this.key, list);
		context.commit(data);
	}
}
