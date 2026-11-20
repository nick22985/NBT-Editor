package com.nick22985.nbteditor.modules.misc;

import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.BlockEntityUtil;
import com.nick22985.nbteditor.util.SkullUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class MiscTab implements EditorTab {
	public static final String ID = "misc";

	private final ModuleContext ctx;
	private final Signature signature;

	public MiscTab(ModuleContext ctx, Signature signature) {
		this.ctx = ctx;
		this.signature = signature;
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
		if (session.kind() == SubjectKind.ITEM) {
			return true;
		}
		return session.kind() == SubjectKind.BLOCK && SkullUtil.isPlayerHead(BlockEntityUtil.blockId(session.root(), false));
	}

	@Override
	public EditorPane create(EditorSession session) {
		return new MiscPane(session, this.ctx, this.signature);
	}
}
