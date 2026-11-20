package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class HelpUtil {
	private HelpUtil() {
	}

	public static List<Text> lines(ModuleContext ctx, String key, int count) {
		List<Text> out = new ArrayList<>();
		out.add(ctx.tr(key + ".title").withStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)));
		for (int line = 1; line <= count; line++) {
			out.add(ctx.tr(key + "." + line).withStyle(Style.EMPTY.withColor(Formatting.GRAY)));
		}
		return out;
	}
}
