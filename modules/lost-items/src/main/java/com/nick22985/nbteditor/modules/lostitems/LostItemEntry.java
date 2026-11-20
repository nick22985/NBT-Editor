package com.nick22985.nbteditor.modules.lostitems;

import com.nick22985.nbteditor.nbt.CompoundTag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record LostItemEntry(LostItemSource source, CompoundTag item, int dataVersion, long time) {
	public static LostItemEntry fromTag(CompoundTag tag) {
		LostItemSource source = LostItemSource.byName(tag.getString("source", ""));
		CompoundTag item = tag.getCompound("item");
		if (source == null || item == null || item.isEmpty()) {
			return null;
		}
		return new LostItemEntry(source, item, tag.getInt("DataVersion", 0), tag.getLong("time", 0));
	}

	public CompoundTag toTag() {
		CompoundTag tag = new CompoundTag();
		tag.putString("source", this.source.serializedName());
		tag.put("item", this.item.copy());
		tag.putInt("DataVersion", this.dataVersion);
		tag.putLong("time", this.time);
		return tag;
	}
}
