package com.nick22985.nbteditor.text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record NbtContent(String path, boolean interpret, Text separator, String sourceKind, String source) implements Content {
	@Override
	public String kind() {
		return "nbt";
	}

	@Override
	public String plain() {
		return "";
	}
}
