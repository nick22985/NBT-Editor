package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextColor;
import com.nick22985.nbteditor.text.TextSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class LegacyTextUtil {
	private LegacyTextUtil() {
	}

	public static Text parse(String input) {
		return parse(input, Formatting.SECTION, true);
	}

	public static Text parse(String input, char marker, boolean hex) {
		List<TextSpan> spans = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		Style style = Style.EMPTY;
		int i = 0;
		while (i < input.length()) {
			char c = input.charAt(i);
			if (c == marker && i + 1 < input.length()) {
				Integer rgb = hex ? readHex(input, i, marker) : null;
				if (rgb != null) {
					flush(spans, current, style);
					style = Style.EMPTY.withColor(TextColor.rgb(rgb));
					i += input.charAt(i + 1) == '#' ? 8 : 14;
					continue;
				}
				Formatting formatting = Formatting.byCode(input.charAt(i + 1));
				if (formatting != null) {
					flush(spans, current, style);
					style = style.apply(formatting);
					i += 2;
					continue;
				}
			}
			current.append(c);
			i++;
		}
		flush(spans, current, style);
		if (spans.isEmpty()) {
			return Text.EMPTY;
		}
		return Text.fromSpans(spans);
	}

	private static Integer readHex(String input, int at, char marker) {
		if (input.charAt(at + 1) == '#' && at + 8 <= input.length()) {
			return parseHex(input.substring(at + 2, at + 8));
		}
		if (Character.toLowerCase(input.charAt(at + 1)) == 'x' && at + 14 <= input.length()) {
			StringBuilder digits = new StringBuilder();
			for (int k = 0; k < 6; k++) {
				int pos = at + 2 + k * 2;
				if (input.charAt(pos) != marker) {
					return null;
				}
				digits.append(input.charAt(pos + 1));
			}
			return parseHex(digits.toString());
		}
		return null;
	}

	private static Integer parseHex(String digits) {
		for (int k = 0; k < digits.length(); k++) {
			if (Character.digit(digits.charAt(k), 16) < 0) {
				return null;
			}
		}
		return Integer.parseInt(digits, 16);
	}

	private static void flush(List<TextSpan> spans, StringBuilder current, Style style) {
		if (current.length() > 0) {
			spans.add(new TextSpan(current.toString(), style));
			current.setLength(0);
		}
	}

	public static String toLegacy(Text text) {
		return toLegacy(text, Formatting.SECTION, false);
	}

	public static String toLegacy(Text text, char marker, boolean hex) {
		StringBuilder out = new StringBuilder();
		Style previous = Style.EMPTY;
		for (TextSpan span : text.flatten()) {
			Style style = span.style();
			if (!sameFormatting(previous, style)) {
				boolean needsReset = removesFlag(previous, style) || (previous.color() != null && style.color() == null);
				if (needsReset || !Objects.equals(previous.color(), style.color())) {
					if (style.color() != null) {
						out.append(colorCode(style.color(), marker, hex));
					} else if (needsReset) {
						out.append(marker).append(Formatting.RESET.code());
					}
					appendFlags(out, Style.EMPTY, style, marker);
				} else {
					appendFlags(out, previous, style, marker);
				}
				previous = style;
			}
			out.append(span.text());
		}
		return out.toString();
	}

	private static String colorCode(TextColor color, char marker, boolean hex) {
		if (color.named() != null || !hex) {
			Formatting formatting = color.named() != null ? color.named() : Formatting.nearestColor(color.rgb());
			return "" + marker + formatting.code();
		}
		String digits = String.format("%06x", color.rgb());
		StringBuilder out = new StringBuilder().append(marker).append('x');
		for (char d : digits.toCharArray()) {
			out.append(marker).append(d);
		}
		return out.toString();
	}

	private static void appendFlags(StringBuilder out, Style from, Style to, char marker) {
		if (to.isObfuscated() && !from.isObfuscated()) {
			out.append(marker).append(Formatting.OBFUSCATED.code());
		}
		if (to.isBold() && !from.isBold()) {
			out.append(marker).append(Formatting.BOLD.code());
		}
		if (to.isStrikethrough() && !from.isStrikethrough()) {
			out.append(marker).append(Formatting.STRIKETHROUGH.code());
		}
		if (to.isUnderlined() && !from.isUnderlined()) {
			out.append(marker).append(Formatting.UNDERLINE.code());
		}
		if (to.isItalic() && !from.isItalic()) {
			out.append(marker).append(Formatting.ITALIC.code());
		}
	}

	private static boolean removesFlag(Style from, Style to) {
		return from.isBold() && !to.isBold() || from.isItalic() && !to.isItalic() || from.isUnderlined() && !to.isUnderlined() || from.isStrikethrough() && !to.isStrikethrough() || from.isObfuscated() && !to.isObfuscated();
	}

	private static boolean sameFormatting(Style a, Style b) {
		return Objects.equals(a.color(), b.color()) && a.isBold() == b.isBold() && a.isItalic() == b.isItalic() && a.isUnderlined() == b.isUnderlined() && a.isStrikethrough() == b.isStrikethrough() && a.isObfuscated() == b.isObfuscated();
	}
}
