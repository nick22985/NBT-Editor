package com.nick22985.nbteditor.modules.blockstates;

import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.BlockEntityUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BlockStatesTab implements EditorTab {
	public static final String ID = "block_states";

	private final ModuleContext ctx;

	public BlockStatesTab(ModuleContext ctx) {
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
		if (kind != SubjectKind.ITEM && kind != SubjectKind.BLOCK) {
			return false;
		}
		return !this.ctx.client().registries().blockProperties(BlockEntityUtil.blockId(session.root(), session.isItem())).isEmpty();
	}

	@Override
	public EditorPane create(EditorSession session) {
		return new BlockStatesPane(session, this.ctx);
	}
}
