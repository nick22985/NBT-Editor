package com.nick22985.nbteditor.modules.enchantments;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EnchantmentsTab implements EditorTab {
	public static final String ID = "enchantments";

	private final ModuleContext ctx;
	private final Setting<Boolean> uncapped;

	public EnchantmentsTab(ModuleContext ctx, Setting<Boolean> uncapped) {
		this.ctx = ctx;
		this.uncapped = uncapped;
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
		return new EnchantmentsPane(session, this.ctx, this.uncapped.get());
	}
}
