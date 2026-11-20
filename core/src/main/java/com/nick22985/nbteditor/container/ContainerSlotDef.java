package com.nick22985.nbteditor.container;

import com.nick22985.nbteditor.ui.SlotIcon;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record ContainerSlotDef(int x, int y, SlotIcon icon) {
	public static ContainerSlotDef of(int x, int y) {
		return new ContainerSlotDef(x, y, SlotIcon.NONE);
	}
}
