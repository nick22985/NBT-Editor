package com.nick22985.nbteditor.modules.editor.nbt;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtTab implements EditorTab {
	public static final String ID = "nbt";

	private final ModuleContext ctx;
	private final NbtClipboard clipboard = new NbtClipboard();
	private final Setting<Integer> highlightLimit;

	public NbtTab(ModuleContext ctx, Setting<Integer> highlightLimit) {
		this.ctx = ctx;
		this.highlightLimit = highlightLimit;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public Text label() {
		return this.ctx.tr("tab.nbt");
	}

	@Override
	public EditorPane create(EditorSession session) {
		return new NbtPane(session, this.ctx, this.clipboard, this.highlightLimit);
	}
}
