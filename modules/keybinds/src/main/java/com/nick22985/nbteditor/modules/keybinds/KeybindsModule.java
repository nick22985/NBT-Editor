package com.nick22985.nbteditor.modules.keybinds;

import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class KeybindsModule implements Module {
	@Override
	public String id() {
		return "keybinds";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		new ItemKeybinds(ctx, ctx.config().bool("inventory_keys", true), ctx.config().bool("key_hints", true)).register();
	}
}
