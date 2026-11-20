package com.nick22985.nbteditor.ui;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record Modifiers(boolean shift, boolean control, boolean alt, boolean superKey) {
	public static final Modifiers NONE = new Modifiers(false, false, false, false);

	public static Modifiers of(boolean shift, boolean control, boolean alt) {
		return new Modifiers(shift, control, alt, false);
	}

	public boolean none() {
		return !this.shift && !this.control && !this.alt && !this.superKey;
	}

	public boolean primary() {
		return this.control || this.superKey;
	}

	public boolean onlyPrimary() {
		return this.primary() && !this.shift && !this.alt;
	}
}
