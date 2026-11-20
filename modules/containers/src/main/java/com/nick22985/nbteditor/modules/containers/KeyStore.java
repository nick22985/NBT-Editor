package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.ui.SlotIcon;
import com.nick22985.nbteditor.util.ContainerUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class KeyStore implements SlotStore {
	private final List<String> keys;
	private final ContainerLayout layout;

	public KeyStore(List<String> keys, ContainerLayout layout) {
		this.keys = List.copyOf(keys);
		this.layout = layout;
	}

	public static KeyStore of(String key) {
		return new KeyStore(List.of(key), ContainerLayout.grid(1, 1));
	}

	public static KeyStore of(String key, SlotIcon icon) {
		return new KeyStore(List.of(key), ContainerLayout.row(List.of(icon)));
	}

	@Override
	public ContainerLayout layout(StoreContext context) {
		return this.layout;
	}

	@Override
	public List<CompoundTag> read(StoreContext context) {
		CompoundTag data = context.data();
		List<CompoundTag> items = new ArrayList<>();
		for (String key : this.keys) {
			items.add(ContainerUtil.readItem(data, key));
		}
		return items;
	}

	@Override
	public void write(StoreContext context, List<CompoundTag> items) {
		CompoundTag data = context.data();
		for (int i = 0; i < this.keys.size(); i++) {
			CompoundTag item = i < items.size() ? items.get(i) : null;
			ContainerUtil.putItem(data, this.keys.get(i), item, context.item(), context.version());
		}
		context.commit(data);
	}
}
