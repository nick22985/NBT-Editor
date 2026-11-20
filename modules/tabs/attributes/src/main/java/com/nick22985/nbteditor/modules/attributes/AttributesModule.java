package com.nick22985.nbteditor.modules.attributes;

import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class AttributesModule implements Module {
	@Override
	public String id() {
		return "attributes";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		ctx.register(EditorTab.REGISTRY, AttributesTab.ID, new AttributesTab(ctx), 30);
		new AttributeCommands(ctx).register();
	}
}
