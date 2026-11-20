package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TextStyleUtil {
	private TextStyleUtil() {
	}

	public static Text colored(Text text, int argb) {
		return text.withStyle(text.style().withColor(argb & 0xFFFFFF));
	}

	public static Text colored(String text, int argb) {
		return colored(Text.literal(text), argb);
	}

	public static Text translatable(String key, int argb, Object... args) {
		return colored(Text.translatable(key, args), argb);
	}
}
