package com.nick22985.nbteditor.modules.blockstates;

import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BlockStatesModule implements Module {
	@Override
	public String id() {
		return "blockstates";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		ctx.register(EditorTab.REGISTRY, BlockStatesTab.ID, new BlockStatesTab(ctx), 80);
	}
}
