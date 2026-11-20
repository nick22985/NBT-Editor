package com.nick22985.nbteditor.keybind;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record KeybindSpec(String id, int defaultKey, String category) {
	public static final int UNBOUND = -1;

	public static KeybindSpec of(String id, int defaultKey) {
		return new KeybindSpec(id, defaultKey, "main");
	}

	public String translationKey() {
		return "key.nbtedit." + this.id;
	}
}
