package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.nbt.CompoundTag;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PotStore implements SlotStore {
	private final SlotStore component = ItemsStore.of(ContainerLayout.grid(1, 1));
	private final SlotStore key = KeyStore.of("item");

	private SlotStore delegate(StoreContext context) {
		return context.containerComponent() ? this.component : this.key;
	}

	@Override
	public ContainerLayout layout(StoreContext context) {
		return this.delegate(context).layout(context);
	}

	@Override
	public List<CompoundTag> read(StoreContext context) {
		return this.delegate(context).read(context);
	}

	@Override
	public void write(StoreContext context, List<CompoundTag> items) {
		this.delegate(context).write(context, items);
	}
}
