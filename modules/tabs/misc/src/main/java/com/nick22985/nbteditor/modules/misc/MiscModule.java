package com.nick22985.nbteditor.modules.misc;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.data.PresetItem;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.util.PresetUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class MiscModule implements Module {
	@Override
	public String id() {
		return "misc";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		Setting<String> stored = ctx.config().string("signature", "").hidden();
		Signature signature = new Signature(stored);
		ctx.register(PresetItem.REGISTRY, "color_codes", new PresetItem(ctx.tr("preset.color_codes"), version -> PresetUtil.colorCodes(ctx.tr("preset.color_codes"), version)));
		ctx.register(EditorTab.REGISTRY, MiscTab.ID, new MiscTab(ctx, signature), 90);
		new MiscCommands(ctx, signature).register();
	}
}
