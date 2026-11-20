package com.nick22985.nbteditor.modules.creativetabs;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.module.Registry;
import com.nick22985.nbteditor.ui.tab.InventoryTab;
import com.nick22985.nbteditor.ui.tab.InventoryTabs;
import com.nick22985.nbteditor.ui.tab.TabPosition;
import com.nick22985.nbteditor.ui.tab.TabScreen;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CreativeTabs implements InventoryTabs {
	private final ModuleContext ctx;
	private final Setting<TabPosition> position;

	public CreativeTabs(ModuleContext ctx) {
		this.ctx = ctx;
		this.position = ctx.config().enumeration("position", TabPosition.BOTTOM_LEFT);
	}

	@Override
	public TabPosition position() {
		return this.position.get();
	}

	@Override
	public List<InventoryTab> shown(TabScreen screen) {
		Registry<InventoryTab> registry = this.ctx.registry(InventoryTab.REGISTRY);
		List<InventoryTab> shown = new ArrayList<>();
		for (InventoryTab tab : registry.values()) {
			try {
				if (tab.shownOn().test(screen)) {
					shown.add(tab);
				}
			} catch (RuntimeException e) {
				this.ctx.log().warn("Inventory tab check failed", e);
			}
		}
		return shown;
	}
}
