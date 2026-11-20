package com.nick22985.nbteditor.modules.clientchest;

import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.container.ItemStash;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.ui.inventory.InventoryScreen;
import com.nick22985.nbteditor.ui.tab.InventoryTab;
import com.nick22985.nbteditor.ui.tab.TabScreen;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.HelpUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ClientChestModule implements Module {
	private ClientChestStore store;

	@Override
	public String id() {
		return "client-chest";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		ClientChestSettings settings = new ClientChestSettings(ctx);
		this.store = new ClientChestStore(ctx);
		ClientChest chest = ctx.services().provide(ClientChest.class, new ClientChest(ctx, this.store, settings));
		ctx.services().provide(ItemStash.class, chest);
		new ClientChestCommands(ctx, chest).register();
		CommandUtil.helpTopic(ctx, CommandContribution.NBT, "clientchest", () -> HelpUtil.lines(ctx, "help.clientchest", 6));
		ctx.register(InventoryTab.REGISTRY, "client_chest", new InventoryTab(() -> ctx.client().items().of("minecraft:barrel", 1), ctx.tr("title"), this::showsTab, chest::open), 10);
	}

	private boolean showsTab(TabScreen screen) {
		return screen.vanillaInventory() || screen.shows(InventoryScreen.class) && !screen.shows(ClientChestScreen.class);
	}

	@Override
	public void shutdown(ModuleContext ctx) {
		if (this.store != null) {
			this.store.stop();
		}
	}
}
