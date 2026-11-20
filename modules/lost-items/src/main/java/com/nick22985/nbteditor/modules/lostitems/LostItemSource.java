package com.nick22985.nbteditor.modules.lostitems;

import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum LostItemSource {
	CURSOR(true),
	DELETED(true),
	OVERWRITTEN(true),
	INVENTORY(true),
	CONTAINER(false),
	PLAYERS(false);

	private final boolean enabledByDefault;

	LostItemSource(boolean enabledByDefault) {
		this.enabledByDefault = enabledByDefault;
	}

	public boolean enabledByDefault() {
		return this.enabledByDefault;
	}

	public String serializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public static LostItemSource byName(String name) {
		for (LostItemSource source : values()) {
			if (source.serializedName().equals(name)) {
				return source;
			}
		}
		return null;
	}
}
