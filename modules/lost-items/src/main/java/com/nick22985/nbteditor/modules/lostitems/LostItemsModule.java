package com.nick22985.nbteditor.modules.lostitems;

import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class LostItemsModule implements Module {
	private LostItemHistory history;

	@Override
	public String id() {
		return "lost-items";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		LostItemSettings settings = new LostItemSettings(ctx);
		this.history = new LostItemHistory(ctx, settings);
		new LostItemTracker(ctx, settings, this.history).register();
		LostItems lost = ctx.services().provide(LostItems.class, new LostItems(ctx, this.history, settings));
		new LostItemCommands(ctx, lost).register();
	}

	@Override
	public void shutdown(ModuleContext ctx) {
		if (this.history != null) {
			this.history.save();
		}
	}
}
