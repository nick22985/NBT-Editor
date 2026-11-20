package com.nick22985.nbteditor.modules.editor;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.modules.editor.nbt.NbtTab;
import com.nick22985.nbteditor.target.TargetOpener;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EditorModule implements Module {
	@Override
	public String id() {
		return "editor";
	}

	@Override
	public boolean canDisable() {
		return false;
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		Setting<Integer> historyLimit = ctx.config().integer("history_limit", 200, 10, 5000);
		Setting<Integer> highlightLimit = ctx.config().integer("highlight_limit", 100000, 0, 10000000);
		ctx.register(EditorTab.REGISTRY, NbtTab.ID, new NbtTab(ctx, highlightLimit), 0);
		EditorOpener opener = new EditorOpener(ctx, historyLimit);
		ctx.services().provide(EditorService.class, opener);
		ctx.services().provide(TargetOpener.class, opener);
	}
}
