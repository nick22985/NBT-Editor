package com.nick22985.nbteditor.text;

import java.util.Locale;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum Formatting {
	BLACK('0', 0x000000),
	DARK_BLUE('1', 0x0000AA),
	DARK_GREEN('2', 0x00AA00),
	DARK_AQUA('3', 0x00AAAA),
	DARK_RED('4', 0xAA0000),
	DARK_PURPLE('5', 0xAA00AA),
	GOLD('6', 0xFFAA00),
	GRAY('7', 0xAAAAAA),
	DARK_GRAY('8', 0x555555),
	BLUE('9', 0x5555FF),
	GREEN('a', 0x55FF55),
	AQUA('b', 0x55FFFF),
	RED('c', 0xFF5555),
	LIGHT_PURPLE('d', 0xFF55FF),
	YELLOW('e', 0xFFFF55),
	WHITE('f', 0xFFFFFF),
	OBFUSCATED('k', -1),
	BOLD('l', -1),
	STRIKETHROUGH('m', -1),
	UNDERLINE('n', -1),
	ITALIC('o', -1),
	RESET('r', -1);

	public static final char SECTION = '§';

	private final char code;
	private final int rgb;

	Formatting(char code, int rgb) {
		this.code = code;
		this.rgb = rgb;
	}

	public char code() {
		return code;
	}

	public int rgb() {
		return rgb;
	}

	public boolean isColor() {
		return rgb >= 0;
	}

	public String serializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static Formatting byCode(char code) {
		char lower = Character.toLowerCase(code);
		for (Formatting formatting : values()) {
			if (formatting.code == lower) {
				return formatting;
			}
		}
		return null;
	}

	public static Formatting byName(String name) {
		for (Formatting formatting : values()) {
			if (formatting.serializedName().equals(name)) {
				return formatting;
			}
		}
		return null;
	}

	public static Formatting nearestColor(int rgb) {
		Formatting best = WHITE;
		long bestDistance = Long.MAX_VALUE;
		for (Formatting formatting : values()) {
			if (!formatting.isColor()) {
				continue;
			}
			int dr = (formatting.rgb >> 16 & 0xFF) - (rgb >> 16 & 0xFF);
			int dg = (formatting.rgb >> 8 & 0xFF) - (rgb >> 8 & 0xFF);
			int db = (formatting.rgb & 0xFF) - (rgb & 0xFF);
			long distance = (long) dr * dr + (long) dg * dg + (long) db * db;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = formatting;
			}
		}
		return best;
	}
}
