package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface Tag {
	TagType type();

	Tag copy();

	default String toSnbt() {
		return SnbtWriter.compact().write(this);
	}
}
