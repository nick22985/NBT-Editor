package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ContainerUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ContainerField implements Field<List<ContainerSlot>> {
	@Override
	public String id() {
		return "container";
	}

	@Override
	public FieldValue<List<ContainerSlot>> read(CompoundTag item, GameVersion version) {
		Tag tag;
		if (Feature.ITEM_COMPONENTS.in(version)) {
			tag = ItemStackUtil.component(item, "container");
		} else {
			CompoundTag data = ItemStackUtil.data(item, version);
			CompoundTag blockEntity = data == null ? null : data.getCompound("BlockEntityTag");
			tag = blockEntity == null ? null : blockEntity.get("Items");
		}
		if (tag == null) {
			return FieldValue.absent();
		}
		if (!(tag instanceof ListTag)) {
			return FieldValue.invalid("not_list");
		}
		return FieldValue.of(ContainerUtil.readList((ListTag) tag, version));
	}

	@Override
	public void write(CompoundTag item, GameVersion version, List<ContainerSlot> value) {
		ListTag list = value == null || value.isEmpty() ? null : ContainerUtil.writeList(value, version);
		if (Feature.ITEM_COMPONENTS.in(version)) {
			ItemStackUtil.setComponent(item, "container", list);
			return;
		}
		CompoundTag data = ItemStackUtil.dataOrCreate(item, version);
		if (list == null) {
			CompoundTag blockEntity = data.getCompound("BlockEntityTag");
			if (blockEntity != null) {
				blockEntity.remove("Items");
				if (blockEntity.isEmpty()) {
					data.remove("BlockEntityTag");
				}
			}
			ItemStackUtil.pruneData(item, version);
		} else {
			data.getOrCreateCompound("BlockEntityTag").put("Items", list);
		}
	}
}
