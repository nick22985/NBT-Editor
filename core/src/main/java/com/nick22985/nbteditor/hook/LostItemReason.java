package com.nick22985.nbteditor.hook;

import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum LostItemReason {
	CURSOR,
	DELETED,
	OVERWRITTEN,
	INVENTORY,
	CONTAINER;

	public String serializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
