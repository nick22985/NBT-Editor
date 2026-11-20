package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.data.ContainerSlot;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.util.ContainerUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class ItemsStore implements SlotStore {
	public static final String ITEMS = "Items";

	private final String key;
	private final ContainerLayout layout;
	private final int legacyOffset;

	public ItemsStore(String key, ContainerLayout layout, int legacyOffset) {
		this.key = key;
		this.layout = layout;
		this.legacyOffset = legacyOffset;
	}

	public static ItemsStore of(ContainerLayout layout) {
		return new ItemsStore(ITEMS, layout, 0);
	}

	@Override
	public ContainerLayout layout(StoreContext context) {
		return this.layout;
	}

	protected int offset(StoreContext context) {
		return Feature.ITEM_COMPONENTS.in(context.version()) ? 0 : this.legacyOffset;
	}

	private boolean component(StoreContext context) {
		return ITEMS.equals(this.key) && context.containerComponent();
	}

	protected List<ContainerSlot> slots(StoreContext context) {
		if (this.component(context)) {
			List<ContainerSlot> slots = ItemFields.CONTAINER.get(context.subject().root(), context.version());
			return slots == null ? List.of() : slots;
		}
		ListTag list = context.data().getList(this.key);
		return list == null ? List.of() : ContainerUtil.readList(list, context.version());
	}

	@Override
	public List<CompoundTag> read(StoreContext context) {
		int size = this.layout.size();
		int offset = this.offset(context);
		List<CompoundTag> items = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			items.add(new CompoundTag());
		}
		boolean[] filled = new boolean[size];
		for (ContainerSlot slot : this.slots(context)) {
			int index = slot.slot() - offset;
			if (index >= 0 && index < size && !filled[index]) {
				items.set(index, slot.item().copy());
				filled[index] = true;
			}
		}
		return items;
	}

	@Override
	public void write(StoreContext context, List<CompoundTag> items) {
		int size = this.layout.size();
		int offset = this.offset(context);
		List<ContainerSlot> slots = new ArrayList<>();
		for (ContainerSlot slot : this.slots(context)) {
			int index = slot.slot() - offset;
			if (index < 0 || index >= size) {
				slots.add(slot);
			}
		}
		for (int i = 0; i < size && i < items.size(); i++) {
			if (!ItemStackUtil.isEmpty(items.get(i))) {
				slots.add(new ContainerSlot(i + offset, items.get(i).copy()));
			}
		}
		slots.sort(Comparator.comparingInt(ContainerSlot::slot));
		if (this.component(context)) {
			ItemFields.CONTAINER.write(context.subject().root(), context.version(), slots);
			return;
		}
		CompoundTag data = context.data();
		data.put(this.key, ContainerUtil.writeSlotted(slots));
		context.commit(data);
	}
}
