package com.nick22985.nbteditor.modules.clientchest;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ClientChestSettings {
	public static final int MAX_PAGES = 1000000;

	private final Setting<Integer> pageLimit;
	private final Setting<Boolean> hideEmpty;

	public ClientChestSettings(ModuleContext ctx) {
		this.pageLimit = ctx.config().integer("page_limit", 0, 0, MAX_PAGES);
		this.hideEmpty = ctx.config().bool("hide_empty", true);
	}

	public int pages() {
		int limit = this.pageLimit.get();
		return limit <= 0 ? MAX_PAGES : limit;
	}

	public boolean unlimited() {
		return this.pageLimit.get() <= 0;
	}

	public boolean hideEmpty() {
		return this.hideEmpty.get();
	}

	public void hideEmpty(boolean hide) {
		this.hideEmpty.set(hide);
	}
}
