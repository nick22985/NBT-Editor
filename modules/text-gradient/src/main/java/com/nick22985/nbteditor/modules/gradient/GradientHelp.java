package com.nick22985.nbteditor.modules.gradient;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class GradientHelp {
	private static final Map<String, String> ENTRIES = new LinkedHashMap<>();

	static {
		ENTRIES.put("tag", "<gradient:#ff5555:gold:aqua>text</gradient>");
		ENTRIES.put("group", "[gradient]{red blue}(text)");
		ENTRIES.put("rainbow", "<rainbow>text</rainbow> <rainbow:120>text</rainbow>");
		ENTRIES.put("paste", "&#ff0000t&#ff8000e&#ffff00x&#80ff00t");
	}

	private final ModuleContext ctx;

	public GradientHelp(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public List<Text> lines() {
		List<Text> out = new ArrayList<>();
		out.add(this.ctx.tr("help.title").withStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)));
		for (Map.Entry<String, String> entry : ENTRIES.entrySet()) {
			out.add(Text.join(List.of(
				Text.literal(entry.getValue(), Style.EMPTY.withColor(Formatting.YELLOW)),
				Text.literal(" - ", Style.EMPTY.withColor(Formatting.DARK_GRAY)),
				this.ctx.tr("help." + entry.getKey()).withStyle(Style.EMPTY.withColor(Formatting.GRAY)))));
		}
		return out;
	}
}
