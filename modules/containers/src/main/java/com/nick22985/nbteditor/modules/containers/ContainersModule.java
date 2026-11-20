package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.command.CommandContribution;
import com.nick22985.nbteditor.container.ContainerFormat;
import com.nick22985.nbteditor.container.ContainerService;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.ui.inventory.InventoryScreen;
import com.nick22985.nbteditor.ui.tab.InventoryTab;
import com.nick22985.nbteditor.ui.tab.TabScreen;
import com.nick22985.nbteditor.util.CommandUtil;
import com.nick22985.nbteditor.util.HelpUtil;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ContainersModule implements Module {
	@Override
	public String id() {
		return "containers";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		int order = 0;
		for (Map.Entry<String, ContainerFormat> entry : new ContainerCatalog().formats().entrySet()) {
			ctx.register(ContainerFormat.REGISTRY, entry.getKey(), entry.getValue(), order++);
		}
		ContainerOpener opener = new ContainerOpener(ctx, id -> ctx.client().registries().entityTraits(id));
		ctx.services().provide(ContainerService.class, opener);
		ctx.register(EditorTab.REGISTRY, ContainerTab.ID, new ContainerTab(ctx, opener), 35);
		ctx.register(InventoryTab.REGISTRY, ContainerCatalog.ENDER_CHEST, new InventoryTab(() -> ctx.client().items().of("minecraft:ender_chest", 1), ctx.tr("ender_chest"), screen -> this.showsEnderChest(screen, opener), opener::openEnderChest), 20);
		new ContainerCommands(ctx, opener).register();
		CommandUtil.helpTopic(ctx, CommandContribution.NBT, "open", () -> HelpUtil.lines(ctx, "help.open", 5));
	}

	private boolean showsEnderChest(TabScreen screen, ContainerOpener opener) {
		if (!screen.vanillaInventory() && !screen.shows(InventoryScreen.class)) {
			return false;
		}
		if (screen.shows(ContainerScreen.class) && ((ContainerScreen) screen.ui()).kind() == SubjectKind.PLAYER) {
			return false;
		}
		return opener.enderChestAvailable();
	}
}
