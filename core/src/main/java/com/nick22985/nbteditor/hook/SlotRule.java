package com.nick22985.nbteditor.hook;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum SlotRule {
	ARMOR,
	SHULKER_BOX,
	FURNACE,
	BREWING_STAND,
	MOUNT,
	BUNDLE;

	public int bit() {
		return 1 << this.ordinal();
	}
}
