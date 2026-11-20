package com.nick22985.nbteditor.modules.display;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum ModelGrouping {
	FOLDER("folder"),
	ITEM_MODEL("item_model"),
	ITEM("item"),
	PACK("pack"),
	NONE("none");

	private final String id;

	ModelGrouping(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}
}
