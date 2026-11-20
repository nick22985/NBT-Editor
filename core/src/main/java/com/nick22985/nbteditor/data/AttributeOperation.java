package com.nick22985.nbteditor.data;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum AttributeOperation {
	ADD_VALUE("add_value"),
	ADD_MULTIPLIED_BASE("add_multiplied_base"),
	ADD_MULTIPLIED_TOTAL("add_multiplied_total");

	private final String serializedName;

	AttributeOperation(String serializedName) {
		this.serializedName = serializedName;
	}

	public String serializedName() {
		return serializedName;
	}

	public static AttributeOperation of(String name) {
		for (AttributeOperation operation : values()) {
			if (operation.serializedName.equals(name)) {
				return operation;
			}
		}
		return null;
	}

	public static AttributeOperation of(int ordinal) {
		return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : null;
	}
}
