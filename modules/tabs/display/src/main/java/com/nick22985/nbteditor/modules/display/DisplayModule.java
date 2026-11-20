package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class DisplayModule implements Module {
	@Override
	public String id() {
		return "display";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		ctx.register(EditorTab.REGISTRY, DisplayTab.ID, new DisplayTab(ctx), 10);
		new DisplayCommands(ctx).register();
		new ModelCommands(ctx).register();
	}
}
