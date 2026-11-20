package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ItemStackUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class UnbreakableField implements Field<Boolean> {
	@Override
	public String id() {
		return "unbreakable";
	}

	@Override
	public FieldValue<Boolean> read(CompoundTag item, GameVersion version) {
		if (Feature.ITEM_COMPONENTS.in(version)) {
			Tag tag = ItemStackUtil.component(item, "unbreakable");
			return tag == null || ItemStackUtil.isComponentRemoved(item, "unbreakable") ? FieldValue.absent() : FieldValue.of(true);
		}
		CompoundTag data = ItemStackUtil.data(item, version);
		if (data == null || !data.contains("Unbreakable")) {
			return FieldValue.absent();
		}
		return FieldValue.of(data.getBoolean("Unbreakable", false));
	}

	@Override
	public void write(CompoundTag item, GameVersion version, Boolean value) {
		boolean on = Boolean.TRUE.equals(value);
		if (Feature.ITEM_COMPONENTS.in(version)) {
			ItemStackUtil.setComponent(item, "unbreakable", on ? new CompoundTag() : null);
			return;
		}
		if (on) {
			ItemStackUtil.dataOrCreate(item, version).putBoolean("Unbreakable", true);
		} else {
			CompoundTag data = ItemStackUtil.data(item, version);
			if (data != null) {
				data.remove("Unbreakable");
			}
			ItemStackUtil.pruneData(item, version);
		}
	}
}
