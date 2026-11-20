package com.nick22985.nbteditor.modules.lostitems;

import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class LostItems {
	private final ModuleContext ctx;
	private final LostItemHistory history;
	private final LostItemSettings settings;

	public LostItems(ModuleContext ctx, LostItemHistory history, LostItemSettings settings) {
		this.ctx = ctx;
		this.history = history;
		this.settings = settings;
	}

	public LostItemHistory history() {
		return this.history;
	}

	public void open(LostItemSource source) {
		this.ctx.platform().scheduler().client(() -> this.ctx.client().screens().open(new LostItemScreen(this.ctx, this.history, this.settings, source)));
	}

	public boolean giveLatest() {
		LostItemEntry latest = this.history.latest();
		return latest != null && this.ctx.client().player().give(this.ctx.client().items().fromTag(latest.item()));
	}
}
