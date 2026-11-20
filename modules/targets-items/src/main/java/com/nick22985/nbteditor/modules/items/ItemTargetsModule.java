package com.nick22985.nbteditor.modules.items;

import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.ItemTargets;
import com.nick22985.nbteditor.target.TargetSource;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ItemTargetsModule implements Module {
	@Override
	public String id() {
		return "targets-items";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		ItemTargets targets = ctx.services().provide(ItemTargets.class, new ItemTargetFactory(ctx));
		ctx.register(TargetSource.REGISTRY, "inventory", new InventorySource(ctx), 0);
		new ItemCommands(ctx, targets).register();
	}
}
