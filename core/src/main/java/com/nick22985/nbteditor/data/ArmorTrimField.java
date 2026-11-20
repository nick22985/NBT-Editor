package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ArmorTrimField implements Field<ArmorTrim> {
	private static final String COMPONENT = "trim";
	private static final String LEGACY = "Trim";
	private static final String MATERIAL = "material";
	private static final String PATTERN = "pattern";

	@Override
	public String id() {
		return COMPONENT;
	}

	@Override
	public boolean supported(GameVersion version) {
		return Feature.TRIMS.in(version);
	}

	private Tag raw(CompoundTag item, GameVersion version) {
		return Feature.ITEM_COMPONENTS.in(version) ? ItemStackUtil.component(item, COMPONENT) : ItemDataUtil.dataTag(item, version, LEGACY);
	}

	@Override
	public FieldValue<ArmorTrim> read(CompoundTag item, GameVersion version) {
		Tag tag = this.raw(item, version);
		if (tag == null) {
			return FieldValue.absent();
		}
		if (!(tag instanceof CompoundTag)) {
			return FieldValue.invalid("not_compound");
		}
		CompoundTag trim = (CompoundTag) tag;
		Tag material = trim.get(MATERIAL);
		Tag pattern = trim.get(PATTERN);
		if (!(material instanceof StringTag) || !(pattern instanceof StringTag)) {
			return FieldValue.invalid("inline");
		}
		return FieldValue.of(new ArmorTrim(ItemStackUtil.namespaced(((StringTag) material).value()), ItemStackUtil.namespaced(((StringTag) pattern).value())));
	}

	@Override
	public void write(CompoundTag item, GameVersion version, ArmorTrim value) {
		boolean components = Feature.ITEM_COMPONENTS.in(version);
		if (value == null) {
			if (components) {
				ItemStackUtil.setComponent(item, COMPONENT, null);
			} else {
				ItemDataUtil.putData(item, version, LEGACY, null);
			}
			return;
		}
		Tag existing = this.raw(item, version);
		CompoundTag trim = existing instanceof CompoundTag ? (CompoundTag) existing.copy() : new CompoundTag();
		trim.putString(MATERIAL, value.material());
		trim.putString(PATTERN, value.pattern());
		if (components) {
			ItemStackUtil.setComponent(item, COMPONENT, trim);
		} else {
			ItemDataUtil.putData(item, version, LEGACY, trim);
		}
	}
}
