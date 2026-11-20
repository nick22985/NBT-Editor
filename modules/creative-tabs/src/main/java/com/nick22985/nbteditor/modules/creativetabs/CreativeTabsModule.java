package com.nick22985.nbteditor.modules.creativetabs;

import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.ui.inventory.InventoryScreen;
import com.nick22985.nbteditor.ui.tab.InventoryTab;
import com.nick22985.nbteditor.ui.tab.InventoryTabs;
import com.nick22985.nbteditor.ui.tab.TabScreen;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CreativeTabsModule implements Module {
	@Override
	public String id() {
		return "creative-tabs";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		ctx.services().provide(InventoryTabs.class, new CreativeTabs(ctx));
		ctx.register(InventoryTab.REGISTRY, "inventory", new InventoryTab(() -> ctx.client().items().of("minecraft:chest", 1), ctx.tr("inventory"), this::modInventory, () -> ctx.client().screens().openInventory()), 0);
	}

	private boolean modInventory(TabScreen screen) {
		return !screen.vanillaInventory() && screen.shows(InventoryScreen.class);
	}
}
