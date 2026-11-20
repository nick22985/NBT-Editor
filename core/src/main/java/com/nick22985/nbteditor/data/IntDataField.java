package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class IntDataField implements Field<Integer> {
	private final String component;
	private final String legacyKey;

	public IntDataField(String component, String legacyKey) {
		this.component = component;
		this.legacyKey = legacyKey;
	}

	@Override
	public String id() {
		return component;
	}

	@Override
	public FieldValue<Integer> read(CompoundTag item, GameVersion version) {
		Tag tag = Feature.ITEM_COMPONENTS.in(version) ? ItemStackUtil.component(item, component) : ItemDataUtil.dataTag(item, version, legacyKey);
		if (tag == null) {
			return FieldValue.absent();
		}
		return tag instanceof NumericTag ? FieldValue.of(((NumericTag) tag).intValue()) : FieldValue.invalid("not_number");
	}

	@Override
	public void write(CompoundTag item, GameVersion version, Integer value) {
		Tag tag = value == null ? null : IntTag.of(value);
		if (Feature.ITEM_COMPONENTS.in(version)) {
			ItemStackUtil.setComponent(item, component, tag);
		} else {
			ItemDataUtil.putData(item, version, legacyKey, tag);
		}
	}
}
