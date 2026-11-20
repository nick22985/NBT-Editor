package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class DisplayTab implements EditorTab {
	public static final String ID = "display";

	private final ModuleContext ctx;

	public DisplayTab(ModuleContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public Text label() {
		return this.ctx.tr("tab");
	}

	@Override
	public boolean applies(EditorSession session) {
		return session.isItem();
	}

	@Override
	public EditorPane create(EditorSession session) {
		return new DisplayPane(session, this.ctx);
	}
}
