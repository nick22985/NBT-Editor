package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ContainerTab implements EditorTab {
	public static final String ID = "container";

	private final ModuleContext ctx;
	private final ContainerOpener opener;

	public ContainerTab(ModuleContext ctx, ContainerOpener opener) {
		this.ctx = ctx;
		this.opener = opener;
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
		return this.opener.format(this.opener.subject(session.kind(), session.root(), session.version())).isPresent();
	}

	@Override
	public EditorPane create(EditorSession session) {
		return new ContainerPane(session, this.ctx, this.opener);
	}
}
