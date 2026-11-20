package com.nick22985.nbteditor.modules.headdb;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class HeadDbSettings {
	private static final int MAX_HOURS = 24 * 365;

	private final Setting<Integer> refreshHours;

	public HeadDbSettings(ModuleContext ctx) {
		this.refreshHours = ctx.config().integer("refresh_hours", 24, 1, MAX_HOURS);
	}

	public long refreshMillis() {
		return this.refreshHours.get() * 3600000L;
	}
}
