package com.nick22985.nbteditor.text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record KeybindContent(String key) implements Content {
	@Override
	public String kind() {
		return "keybind";
	}

	@Override
	public String plain() {
		return key;
	}
}
