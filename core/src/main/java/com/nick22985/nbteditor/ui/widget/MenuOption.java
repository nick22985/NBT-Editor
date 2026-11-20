package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record MenuOption(Text label, String chip, int chipColor, Runnable action, boolean enabled, boolean checked, Text tooltip) {
	public static MenuOption of(Text label, Runnable action) {
		return new MenuOption(label, null, 0, action, true, false, null);
	}

	public static MenuOption separator() {
		return new MenuOption(null, null, 0, null, false, false, null);
	}

	public boolean isSeparator() {
		return this.label == null;
	}

	public MenuOption chip(String chip, int chipColor) {
		return new MenuOption(this.label, chip, chipColor, this.action, this.enabled, this.checked, this.tooltip);
	}

	public MenuOption enabled(boolean enabled) {
		return new MenuOption(this.label, this.chip, this.chipColor, this.action, enabled, this.checked, this.tooltip);
	}

	public MenuOption checked(boolean checked) {
		return new MenuOption(this.label, this.chip, this.chipColor, this.action, this.enabled, checked, this.tooltip);
	}

	public MenuOption tooltip(Text tooltip) {
		return new MenuOption(this.label, this.chip, this.chipColor, this.action, this.enabled, this.checked, tooltip);
	}
}
