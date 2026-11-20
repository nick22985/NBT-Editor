package com.nick22985.nbteditor.ui.tab;

import com.nick22985.nbteditor.ui.UiScreen;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record TabScreen(TabScreenKind kind, UiScreen ui) {
	public boolean vanillaInventory() {
		return this.kind != TabScreenKind.MOD_SCREEN;
	}

	public boolean shows(Class<?> type) {
		return type.isInstance(this.ui);
	}
}
