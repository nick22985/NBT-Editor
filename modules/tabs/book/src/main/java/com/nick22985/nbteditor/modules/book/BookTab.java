package com.nick22985.nbteditor.modules.book;

import com.nick22985.nbteditor.data.BookField;
import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.ItemStackUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BookTab implements EditorTab {
	public static final String ID = "book";

	private final ModuleContext ctx;

	public BookTab(ModuleContext ctx) {
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
		if (!session.isItem()) {
			return false;
		}
		String id = ItemStackUtil.id(session.root());
		return BookField.WRITTEN.equals(id) || BookField.WRITABLE.equals(id);
	}

	@Override
	public EditorPane create(EditorSession session) {
		return new BookPane(session, this.ctx);
	}
}
