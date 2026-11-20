package com.nick22985.nbteditor.ui;

import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum SlotIcon {
	NONE,
	HELMET,
	CHESTPLATE,
	LEGGINGS,
	BOOTS,
	SHIELD,
	SWORD,
	SADDLE,
	HORSE_ARMOR,
	LLAMA_ARMOR,
	POTION,
	BREWING_FUEL;

	public String serializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
