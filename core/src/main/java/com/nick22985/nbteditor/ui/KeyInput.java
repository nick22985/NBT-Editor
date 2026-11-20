package com.nick22985.nbteditor.ui;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record KeyInput(int key, int scancode, Modifiers modifiers) {
	public boolean is(int key) {
		return this.key == key;
	}

	public boolean isEnter() {
		return this.key == Key.ENTER || this.key == Key.KP_ENTER;
	}

	public boolean shortcut(int key) {
		return this.key == key && this.modifiers.onlyPrimary();
	}

	public boolean shiftShortcut(int key) {
		return this.key == key && this.modifiers.primary() && this.modifiers.shift() && !this.modifiers.alt();
	}

	public boolean plain(int key) {
		return this.key == key && this.modifiers.none();
	}
}
