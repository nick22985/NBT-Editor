package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextDialect;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.TextCodecUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ItemNameField implements Field<Text> {
	@Override
	public String id() {
		return "item_name";
	}

	@Override
	public boolean supported(GameVersion version) {
		return Feature.ITEM_COMPONENTS.in(version);
	}

	@Override
	public FieldValue<Text> read(CompoundTag item, GameVersion version) {
		return ItemDataUtil.readText(ItemStackUtil.component(item, "item_name"), version);
	}

	@Override
	public void write(CompoundTag item, GameVersion version, Text value) {
		ItemStackUtil.setComponent(item, "item_name", value == null ? null : TextCodecUtil.toStored(value, TextDialect.of(version)));
	}
}
