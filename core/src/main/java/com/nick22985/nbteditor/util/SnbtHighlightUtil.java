package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.SnbtException;
import com.nick22985.nbteditor.nbt.SnbtReader;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SnbtHighlightUtil {
	private SnbtHighlightUtil() {
	}

	public static Text highlight(String snbt, SnbtDialect dialect) {
		return highlight(snbt, Integer.MAX_VALUE, dialect);
	}

	public static Text highlight(String snbt, int maxLength, SnbtDialect dialect) {
		String input = snbt.length() > maxLength ? snbt.substring(0, maxLength) : snbt;
		List<Text> parts = new ArrayList<>();
		StringBuilder plain = new StringBuilder();
		int length = input.length();
		int i = 0;
		while (i < length) {
			char c = input.charAt(i);
			if (c == '"' || c == '\'') {
				int end = quotedEnd(input, i);
				flush(parts, plain);
				parts.add(TextStyleUtil.colored(input.substring(i, end), isKey(input, end) ? Theme.AQUA : Theme.GREEN));
				i = end;
			} else if (SnbtUtil.isUnquotedChar(c)) {
				int end = i;
				while (end < length && SnbtUtil.isUnquotedChar(input.charAt(end))) {
					end++;
				}
				flush(parts, plain);
				token(parts, input.substring(i, end), isKey(input, end), end < length && input.charAt(end) == ';', dialect);
				i = end;
			} else {
				plain.append(c);
				i++;
			}
		}
		flush(parts, plain);
		return Text.join(parts);
	}

	public static Text number(String token) {
		int split = token.length();
		char last = token.isEmpty() ? '0' : token.charAt(token.length() - 1);
		if (Character.isLetter(last)) {
			split = token.length() - 1;
			while (split > 0 && Character.isLetter(token.charAt(split - 1)) && token.charAt(split - 1) != 'e' && token.charAt(split - 1) != 'E') {
				split--;
			}
		}
		if (split >= token.length()) {
			return TextStyleUtil.colored(token, Theme.GOLD);
		}
		return Text.join(List.of(TextStyleUtil.colored(token.substring(0, split), Theme.GOLD), TextStyleUtil.colored(token.substring(split), Theme.RED)));
	}

	private static void token(List<Text> parts, String token, boolean key, boolean arrayPrefix, SnbtDialect dialect) {
		if (key) {
			parts.add(TextStyleUtil.colored(token, Theme.AQUA));
		} else if (arrayPrefix) {
			parts.add(TextStyleUtil.colored(token, Theme.RED));
		} else if (isNumber(token) && !(dialect == SnbtDialect.LEGACY && readsAsString(token))) {
			parts.add(number(token));
		} else if (token.equals("true") || token.equals("false")) {
			parts.add(TextStyleUtil.colored(token, Theme.GOLD));
		} else {
			parts.add(TextStyleUtil.colored(token, Theme.GREEN));
		}
	}

	private static boolean isNumber(String token) {
		int i = 0;
		if (i < token.length() && (token.charAt(i) == '-' || token.charAt(i) == '+')) {
			i++;
		}
		return i < token.length() && (Character.isDigit(token.charAt(i)) || token.charAt(i) == '.' && i + 1 < token.length() && Character.isDigit(token.charAt(i + 1)));
	}

	private static boolean readsAsString(String token) {
		try {
			return new SnbtReader(token, SnbtDialect.LEGACY).read() instanceof StringTag;
		} catch (SnbtException e) {
			return true;
		}
	}

	private static int quotedEnd(String input, int start) {
		char quote = input.charAt(start);
		int i = start + 1;
		while (i < input.length()) {
			char c = input.charAt(i);
			if (c == '\\') {
				i += 2;
				continue;
			}
			i++;
			if (c == quote) {
				break;
			}
		}
		return Math.min(i, input.length());
	}

	private static boolean isKey(String input, int end) {
		int i = end;
		while (i < input.length() && Character.isWhitespace(input.charAt(i))) {
			i++;
		}
		return i < input.length() && input.charAt(i) == ':';
	}

	private static void flush(List<Text> parts, StringBuilder plain) {
		if (plain.length() > 0) {
			parts.add(TextStyleUtil.colored(plain.toString(), Theme.TEXT));
			plain.setLength(0);
		}
	}
}
