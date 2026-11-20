package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextDialect;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.TextCodecUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CustomNameField implements Field<Text> {
	private final String component;
	private final String displayKey;

	public CustomNameField(String component, String displayKey) {
		this.component = component;
		this.displayKey = displayKey;
	}

	@Override
	public String id() {
		return component;
	}

	@Override
	public FieldValue<Text> read(CompoundTag item, GameVersion version) {
		if (Feature.ITEM_COMPONENTS.in(version)) {
			return ItemDataUtil.readText(ItemStackUtil.component(item, component), version);
		}
		CompoundTag display = ItemDataUtil.display(item, false);
		return ItemDataUtil.readText(display == null ? null : display.get(displayKey), version);
	}

	@Override
	public void write(CompoundTag item, GameVersion version, Text value) {
		Tag stored = value == null ? null : TextCodecUtil.toStored(value, TextDialect.of(version));
		if (Feature.ITEM_COMPONENTS.in(version)) {
			ItemStackUtil.setComponent(item, component, stored);
		} else {
			ItemDataUtil.putDisplay(item, version, displayKey, stored);
		}
	}
}
