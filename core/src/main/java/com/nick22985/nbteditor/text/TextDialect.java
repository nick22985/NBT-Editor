package com.nick22985.nbteditor.text;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum TextDialect {
	JSON_TAG_ITEMS(false, false),
	JSON_COMPONENT_ITEMS(false, true),
	NBT(true, true);

	public static final TextDialect LATEST = NBT;

	private final boolean snakeCaseEvents;
	private final boolean componentItems;

	TextDialect(boolean snakeCaseEvents, boolean componentItems) {
		this.snakeCaseEvents = snakeCaseEvents;
		this.componentItems = componentItems;
	}

	public static TextDialect of(GameVersion version) {
		if (Feature.SNAKE_CASE_TEXT_EVENTS.in(version)) {
			return NBT;
		}
		if (Feature.ITEM_COMPONENTS.in(version)) {
			return JSON_COMPONENT_ITEMS;
		}
		return JSON_TAG_ITEMS;
	}

	public boolean snakeCaseEvents() {
		return snakeCaseEvents;
	}

	public boolean componentItems() {
		return componentItems;
	}

	public boolean storesAsNbt() {
		return this == NBT;
	}
}
