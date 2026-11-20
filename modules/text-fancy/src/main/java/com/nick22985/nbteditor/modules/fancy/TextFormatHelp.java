package com.nick22985.nbteditor.modules.fancy;

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
public final class TextFormatHelp {
	private static final Map<String, String> ENTRIES = new LinkedHashMap<>();

	static {
		ENTRIES.put("codes", "&0-&f &l &o &n &m &k &r");
		ENTRIES.put("hex", "&#RRGGBB;");
		ENTRIES.put("spigot", "&x&R&R&G&G&B&B");
		ENTRIES.put("shadow", "&_c &_#RRGGBB &_r");
		ENTRIES.put("tags", "<bold>..</bold> <red> <#ff8800> <color:gold>");
		ENTRIES.put("short", "<b> <i> <u> <st> <obf> <c:..> <reset> <br>");
		ENTRIES.put("click", "[open_url]{https://..}(text)");
		ENTRIES.put("hover", "[show_text]{Hi}(text)");
		ENTRIES.put("hover_item", "[show_item]{diamond 2 {..}}(text)");
		ENTRIES.put("hover_entity", "[show_entity]{pig <uuid> Name}(text)");
		ENTRIES.put("insertion", "[insertion]{..}(text) [font]{minecraft:alt}(text)");
		ENTRIES.put("atoms", "[translate]{key|arg} [keybind]{key.jump}");
		ENTRIES.put("atoms_more", "[score]{name|objective} [selector]{@p} [nbt]{entity|@s|Path}");
		ENTRIES.put("escape", "\\& \\[ \\< \\\\");
	}

	private final ModuleContext ctx;

	public TextFormatHelp(ModuleContext ctx) {
		this.ctx = ctx;
	}

	public List<Text> lines() {
		List<Text> out = new ArrayList<>();
		out.add(this.ctx.tr("textformat.title").withStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)));
		for (Map.Entry<String, String> entry : ENTRIES.entrySet()) {
			out.add(Text.join(List.of(
				Text.literal(entry.getValue(), Style.EMPTY.withColor(Formatting.YELLOW)),
				Text.literal(" - ", Style.EMPTY.withColor(Formatting.DARK_GRAY)),
				this.ctx.tr("textformat." + entry.getKey()).withStyle(Style.EMPTY.withColor(Formatting.GRAY)))));
		}
		return out;
	}
}
