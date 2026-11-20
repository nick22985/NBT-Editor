package com.nick22985.nbteditor.modules.potion;

import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PotionTab implements EditorTab {
	public static final String ID = "potion";
	private static final Set<String> POTION_ITEMS = Set.of("minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow");

	private final ModuleContext ctx;

	public PotionTab(ModuleContext ctx) {
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
		return POTION_ITEMS.contains(id) || PotionPane.STEW.equals(id) || ItemFields.POTION_CONTENTS.has(session.root(), session.version()) || ItemFields.STEW_EFFECTS.has(session.root(), session.version());
	}

	@Override
	public EditorPane create(EditorSession session) {
		return new PotionPane(session, this.ctx);
	}
}
