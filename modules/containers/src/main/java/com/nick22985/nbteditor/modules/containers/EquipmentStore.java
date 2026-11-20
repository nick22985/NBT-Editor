package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerLayout;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.ui.SlotIcon;
import com.nick22985.nbteditor.util.ContainerUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EquipmentStore implements SlotStore {
	public static final String EQUIPMENT = "equipment";
	public static final String ARMOR_ITEMS = "ArmorItems";
	public static final String HAND_ITEMS = "HandItems";
	public static final String BODY_ARMOR = "body_armor_item";
	public static final String SADDLE_ITEM = "SaddleItem";
	private static final int COLUMN_GAP = 4;

	private final List<EquipmentSlot> slots;
	private final String legacyBody;
	private final ContainerLayout layout;

	public EquipmentStore(Set<EquipmentSlot> slots, String legacyBody, SlotIcon bodyIcon) {
		EnumSet<EquipmentSlot> sorted = slots.isEmpty() ? EnumSet.noneOf(EquipmentSlot.class) : EnumSet.copyOf(slots);
		this.slots = List.copyOf(sorted);
		this.legacyBody = legacyBody;
		List<ContainerLayout> columns = new ArrayList<>();
		for (int column = 0; column < 3; column++) {
			List<SlotIcon> icons = new ArrayList<>();
			for (EquipmentSlot slot : this.slots) {
				if (slot.column() == column) {
					icons.add(slot == EquipmentSlot.BODY ? bodyIcon : slot.icon());
				}
			}
			columns.add(ContainerLayout.column(icons));
		}
		this.layout = ContainerLayout.beside(COLUMN_GAP, columns);
	}

	public List<EquipmentSlot> slots() {
		return this.slots;
	}

	@Override
	public ContainerLayout layout(StoreContext context) {
		return this.layout;
	}

	private String legacyKey(EquipmentSlot slot, StoreContext context) {
		if (slot == EquipmentSlot.SADDLE) {
			return SADDLE_ITEM;
		}
		return Feature.BODY_ARMOR_ITEM.in(context.version()) ? BODY_ARMOR : this.legacyBody;
	}

	@Override
	public List<CompoundTag> read(StoreContext context) {
		CompoundTag data = context.data();
		boolean modern = Feature.ENTITY_EQUIPMENT.in(context.version());
		CompoundTag equipment = data.getCompound(EQUIPMENT);
		List<CompoundTag> items = new ArrayList<>();
		for (EquipmentSlot slot : this.slots) {
			if (modern) {
				items.add(equipment == null ? new CompoundTag() : ContainerUtil.readItem(equipment, slot.serializedName()));
			} else if (slot.armor() || slot.hand()) {
				ListTag list = data.getList(slot.armor() ? ARMOR_ITEMS : HAND_ITEMS);
				CompoundTag item = list == null || slot.legacyIndex() >= list.size() ? null : list.getCompound(slot.legacyIndex());
				items.add(item == null ? new CompoundTag() : item.copy());
			} else {
				String key = this.legacyKey(slot, context);
				items.add(key == null ? new CompoundTag() : ContainerUtil.readItem(data, key));
			}
		}
		return items;
	}

	@Override
	public void write(StoreContext context, List<CompoundTag> items) {
		CompoundTag data = context.data();
		if (Feature.ENTITY_EQUIPMENT.in(context.version())) {
			CompoundTag equipment = data.getCompound(EQUIPMENT);
			equipment = equipment == null ? new CompoundTag() : equipment.copy();
			for (int i = 0; i < this.slots.size(); i++) {
				CompoundTag item = i < items.size() ? items.get(i) : null;
				String key = this.slots.get(i).serializedName();
				if (ItemStackUtil.isEmpty(item)) {
					equipment.remove(key);
				} else {
					equipment.put(key, item.copy());
				}
			}
			if (equipment.isEmpty()) {
				data.remove(EQUIPMENT);
			} else {
				data.put(EQUIPMENT, equipment);
			}
			context.commit(data);
			return;
		}
		for (int i = 0; i < this.slots.size(); i++) {
			EquipmentSlot slot = this.slots.get(i);
			CompoundTag item = i < items.size() ? items.get(i) : null;
			if (slot.armor() || slot.hand()) {
				this.writeListed(data, slot.armor() ? ARMOR_ITEMS : HAND_ITEMS, slot.armor() ? 4 : 2, slot.legacyIndex(), item);
			} else {
				String key = this.legacyKey(slot, context);
				if (key != null) {
					ContainerUtil.putItem(data, key, item, context.item(), context.version());
				}
			}
		}
		context.commit(data);
	}

	private void writeListed(CompoundTag data, String key, int size, int index, CompoundTag item) {
		ListTag existing = data.getList(key);
		ListTag list = existing == null ? new ListTag() : existing.copy();
		while (list.size() < size) {
			list.add(new CompoundTag());
		}
		list.set(index, ItemStackUtil.isEmpty(item) ? new CompoundTag() : item.copy());
		boolean empty = true;
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			if (entry != null && !entry.isEmpty()) {
				empty = false;
			}
		}
		if (empty && existing == null) {
			return;
		}
		data.put(key, list);
	}
}
