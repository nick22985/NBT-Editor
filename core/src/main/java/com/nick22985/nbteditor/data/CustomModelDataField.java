package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.FloatTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CustomModelDataField implements Field<Integer> {
	@Override
	public String id() {
		return "custom_model_data";
	}

	@Override
	public FieldValue<Integer> read(CompoundTag item, GameVersion version) {
		Tag tag = Feature.ITEM_COMPONENTS.in(version) ? ItemStackUtil.component(item, "custom_model_data") : ItemDataUtil.dataTag(item, version, "CustomModelData");
		if (tag == null) {
			return FieldValue.absent();
		}
		if (tag instanceof NumericTag) {
			return FieldValue.of(((NumericTag) tag).intValue());
		}
		if (tag instanceof CompoundTag) {
			ListTag floats = ((CompoundTag) tag).getList("floats");
			if (floats == null || floats.isEmpty()) {
				return FieldValue.of(null);
			}
			return floats.get(0) instanceof NumericTag ? FieldValue.of((int) ((NumericTag) floats.get(0)).floatValue()) : FieldValue.invalid("not_number");
		}
		return FieldValue.invalid("not_number");
	}

	@Override
	public void write(CompoundTag item, GameVersion version, Integer value) {
		if (!Feature.ITEM_COMPONENTS.in(version)) {
			ItemDataUtil.putData(item, version, "CustomModelData", value == null ? null : IntTag.of(value));
			return;
		}
		if (value == null) {
			ItemStackUtil.setComponent(item, "custom_model_data", null);
		} else if (Feature.CUSTOM_MODEL_DATA_LISTS.in(version)) {
			Tag existing = ItemStackUtil.component(item, "custom_model_data");
			CompoundTag compound = existing instanceof CompoundTag ? (CompoundTag) existing.copy() : new CompoundTag();
			ListTag floats = compound.getOrCreateList("floats");
			if (floats.isEmpty()) {
				floats.add(FloatTag.of(value));
			} else {
				floats.set(0, FloatTag.of(value));
			}
			ItemStackUtil.setComponent(item, "custom_model_data", compound);
		} else {
			ItemStackUtil.setComponent(item, "custom_model_data", IntTag.of(value));
		}
	}
}
