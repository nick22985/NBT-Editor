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
public final class DyedColorField implements Field<Integer> {
	@Override
	public String id() {
		return "dyed_color";
	}

	@Override
	public FieldValue<Integer> read(CompoundTag item, GameVersion version) {
		Tag tag;
		if (Feature.ITEM_COMPONENTS.in(version)) {
			tag = ItemStackUtil.component(item, "dyed_color");
			if (tag instanceof CompoundTag) {
				tag = ((CompoundTag) tag).get("rgb");
			}
		} else {
			CompoundTag display = ItemDataUtil.display(item, false);
			tag = display == null ? null : display.get("color");
		}
		if (tag == null) {
			return FieldValue.absent();
		}
		return tag instanceof NumericTag ? FieldValue.of(((NumericTag) tag).intValue() & 0xFFFFFF) : FieldValue.invalid("not_number");
	}

	@Override
	public void write(CompoundTag item, GameVersion version, Integer value) {
		if (!Feature.ITEM_COMPONENTS.in(version)) {
			ItemDataUtil.putDisplay(item, version, "color", value == null ? null : IntTag.of(value));
			return;
		}
		if (value == null) {
			ItemStackUtil.setComponent(item, "dyed_color", null);
		} else if (Feature.TOOLTIP_DISPLAY.in(version)) {
			ItemStackUtil.setComponent(item, "dyed_color", IntTag.of(value));
		} else {
			Tag existing = ItemStackUtil.component(item, "dyed_color");
			CompoundTag compound = existing instanceof CompoundTag ? (CompoundTag) existing.copy() : new CompoundTag();
			compound.putInt("rgb", value);
			ItemStackUtil.setComponent(item, "dyed_color", compound);
		}
	}
}
