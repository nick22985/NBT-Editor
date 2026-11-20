package com.nick22985.nbteditor.modules.display;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum ModelSorting {
	NAME("name"),
	VALUE("value"),
	PACK_ORDER("pack_order");

	private final String id;

	ModelSorting(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}
}
