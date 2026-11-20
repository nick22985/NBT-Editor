package com.nick22985.nbteditor.modules.attributes;

import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class AttributesTab implements EditorTab {
	public static final String ID = "attributes";

	private final ModuleContext ctx;

	public AttributesTab(ModuleContext ctx) {
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
		SubjectKind kind = session.kind();
		return kind == SubjectKind.ITEM || kind == SubjectKind.ENTITY || kind == SubjectKind.PLAYER;
	}

	@Override
	public EditorPane create(EditorSession session) {
		return session.isItem() ? new ModifiersPane(session, this.ctx) : new BaseAttributesPane(session, this.ctx);
	}
}
