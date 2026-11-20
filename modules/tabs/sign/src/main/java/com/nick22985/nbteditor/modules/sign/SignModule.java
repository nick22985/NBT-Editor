package com.nick22985.nbteditor.modules.sign;

import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SignModule implements Module {
	@Override
	public String id() {
		return "sign";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		ctx.register(EditorTab.REGISTRY, SignTab.ID, new SignTab(ctx), 15);
		new SignCommands(ctx).register();
	}
}
