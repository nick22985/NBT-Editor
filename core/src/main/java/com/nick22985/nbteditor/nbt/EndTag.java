package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EndTag implements Tag {
	public static final EndTag INSTANCE = new EndTag();

	private EndTag() {
	}

	@Override
	public TagType type() {
		return TagType.END;
	}

	@Override
	public Tag copy() {
		return this;
	}
}
