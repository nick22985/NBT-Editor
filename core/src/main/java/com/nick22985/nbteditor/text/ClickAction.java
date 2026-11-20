package com.nick22985.nbteditor.text;

import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum ClickAction {
	OPEN_URL,
	OPEN_FILE,
	RUN_COMMAND,
	SUGGEST_COMMAND,
	CHANGE_PAGE,
	COPY_TO_CLIPBOARD,
	SHOW_DIALOG,
	CUSTOM;

	public String serializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static ClickAction byName(String name) {
		for (ClickAction action : values()) {
			if (action.serializedName().equals(name)) {
				return action;
			}
		}
		return null;
	}
}
