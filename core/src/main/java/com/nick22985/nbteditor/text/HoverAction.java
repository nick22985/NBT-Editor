package com.nick22985.nbteditor.text;

import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum HoverAction {
	SHOW_TEXT,
	SHOW_ITEM,
	SHOW_ENTITY;

	public String serializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static HoverAction byName(String name) {
		for (HoverAction action : values()) {
			if (action.serializedName().equals(name)) {
				return action;
			}
		}
		return null;
	}
}
