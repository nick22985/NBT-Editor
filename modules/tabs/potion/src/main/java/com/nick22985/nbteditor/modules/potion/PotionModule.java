package com.nick22985.nbteditor.modules.potion;

import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PotionModule implements Module {
	@Override
	public String id() {
		return "potion";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		ctx.register(EditorTab.REGISTRY, PotionTab.ID, new PotionTab(ctx), 60);
		new PotionCommands(ctx).register();
	}
}
