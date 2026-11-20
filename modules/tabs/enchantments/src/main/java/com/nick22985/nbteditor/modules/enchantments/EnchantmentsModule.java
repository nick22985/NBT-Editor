package com.nick22985.nbteditor.modules.enchantments;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EnchantmentsModule implements Module {
	@Override
	public String id() {
		return "enchantments";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		Setting<Boolean> uncapped = ctx.config().bool("uncapped_levels", false);
		ctx.register(EditorTab.REGISTRY, EnchantmentsTab.ID, new EnchantmentsTab(ctx, uncapped), 20);
		new EnchantmentCommands(ctx).register();
		new BookApplier(ctx, ctx.config().bool("book_apply", true)).register();
	}
}
