package com.nick22985.nbteditor.modules.sign;

import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.BlockEntityUtil;
import com.nick22985.nbteditor.util.SignUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SignTab implements EditorTab {
	public static final String ID = "sign";

	private final ModuleContext ctx;

	public SignTab(ModuleContext ctx) {
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
		return (kind == SubjectKind.ITEM || kind == SubjectKind.BLOCK) && SignUtil.isSign(BlockEntityUtil.blockId(session.root(), session.isItem()));
	}

	@Override
	public EditorPane create(EditorSession session) {
		return new SignPane(session, this.ctx);
	}
}
