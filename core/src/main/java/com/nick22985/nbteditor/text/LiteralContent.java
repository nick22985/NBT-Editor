package com.nick22985.nbteditor.text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record LiteralContent(String text) implements Content {
	public static final LiteralContent EMPTY = new LiteralContent("");

	@Override
	public String kind() {
		return "text";
	}

	@Override
	public String plain() {
		return text;
	}
}
