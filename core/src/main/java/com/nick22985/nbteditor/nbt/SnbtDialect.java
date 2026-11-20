package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum SnbtDialect {
	LEGACY,
	MODERN;

	public boolean allowsMixedLists() {
		return this == MODERN;
	}
}
