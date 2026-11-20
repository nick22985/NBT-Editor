package com.nick22985.nbteditor.modules.armor;

import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ArmorTab implements EditorTab {
	public static final String ID = "armor";

	private final ModuleContext ctx;

	public ArmorTab(ModuleContext ctx) {
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
		return new ArmorPane(session, this.ctx);
	}
}
