package com.nick22985.nbteditor.ui;

import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.target.Target;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record HoveredItem(GameItem item, Target target, int playerSlot) {
	public static final int NO_SLOT = -1;
}
