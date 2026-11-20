package com.nick22985.nbteditor.text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record SelectorContent(String pattern, Text separator) implements Content {
	@Override
	public String kind() {
		return "selector";
	}

	@Override
	public String plain() {
		return pattern;
	}
}
