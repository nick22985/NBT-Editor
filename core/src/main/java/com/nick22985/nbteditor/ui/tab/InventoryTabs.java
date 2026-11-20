package com.nick22985.nbteditor.ui.tab;

import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public interface InventoryTabs {
	TabPosition position();

	List<InventoryTab> shown(TabScreen screen);
}
