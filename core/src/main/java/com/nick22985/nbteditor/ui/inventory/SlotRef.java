package com.nick22985.nbteditor.ui.inventory;

import com.nick22985.nbteditor.platform.GameItem;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record SlotRef(SlotSection section, int index) {
	public GameItem item() {
		return this.section.get(this.index);
	}

	public boolean writable() {
		return this.section.writable(this.index);
	}
}
