package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BundleField implements Field<List<CompoundTag>> {
	@Override
	public String id() {
		return "bundle_contents";
	}

	@Override
	public FieldValue<List<CompoundTag>> read(CompoundTag item, GameVersion version) {
		Tag tag = Feature.ITEM_COMPONENTS.in(version) ? ItemStackUtil.component(item, "bundle_contents") : ItemDataUtil.dataTag(item, version, "Items");
		if (tag == null) {
			return FieldValue.absent();
		}
		if (!(tag instanceof ListTag)) {
			return FieldValue.invalid("not_list");
		}
		List<CompoundTag> items = new ArrayList<>();
		for (Tag element : (ListTag) tag) {
			if (element instanceof CompoundTag) {
				items.add((CompoundTag) element);
			}
		}
		return FieldValue.of(items);
	}

	@Override
	public void write(CompoundTag item, GameVersion version, List<CompoundTag> value) {
		ListTag list = value == null ? null : new ListTag(value);
		if (Feature.ITEM_COMPONENTS.in(version)) {
			ItemStackUtil.setComponent(item, "bundle_contents", list);
		} else {
			ItemDataUtil.putData(item, version, "Items", list);
		}
	}
}
