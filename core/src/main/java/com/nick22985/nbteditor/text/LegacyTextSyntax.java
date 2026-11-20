package com.nick22985.nbteditor.text;

import com.nick22985.nbteditor.util.LegacyTextUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class LegacyTextSyntax implements TextSyntax {
	public static final LegacyTextSyntax INSTANCE = new LegacyTextSyntax();

	private LegacyTextSyntax() {
	}

	@Override
	public Text parse(String input) {
		return LegacyTextUtil.parse(input.replace(Formatting.SECTION, '&'), '&', true);
	}

	@Override
	public String stringify(Text text) {
		return LegacyTextUtil.toLegacy(text, '&', true);
	}
}
