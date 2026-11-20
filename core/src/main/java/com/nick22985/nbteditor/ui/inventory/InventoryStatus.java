package com.nick22985.nbteditor.ui.inventory;

import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record InventoryStatus(Text text, int color, String[] sprite) {
	public static InventoryStatus of(Text text, int color) {
		return new InventoryStatus(text, color, null);
	}
}
