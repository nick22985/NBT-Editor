package com.nick22985.nbteditor.util;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TextEditUtil {
	private TextEditUtil() {
	}

	private static boolean isWordChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}

	public static int wordLeft(String text, int position) {
		int i = Math.min(position, text.length());
		while (i > 0 && !isWordChar(text.charAt(i - 1))) {
			i--;
		}
		while (i > 0 && isWordChar(text.charAt(i - 1))) {
			i--;
		}
		return i;
	}

	public static int wordRight(String text, int position) {
		int i = Math.max(0, position);
		int length = text.length();
		while (i < length && !isWordChar(text.charAt(i))) {
			i++;
		}
		while (i < length && isWordChar(text.charAt(i))) {
			i++;
		}
		return i;
	}

	public static int[] wordAt(String text, int position) {
		int start = Math.max(0, Math.min(position, text.length()));
		int end = start;
		if (start < text.length() && isWordChar(text.charAt(start)) || start > 0 && isWordChar(text.charAt(start - 1))) {
			while (start > 0 && isWordChar(text.charAt(start - 1))) {
				start--;
			}
			while (end < text.length() && isWordChar(text.charAt(end))) {
				end++;
			}
		} else if (end < text.length()) {
			end++;
		}
		return new int[] {start, end};
	}

	public static int lineStart(String text, int position) {
		int index = text.lastIndexOf('\n', Math.max(0, position - 1));
		return position == 0 ? 0 : index + 1;
	}

	public static int lineEnd(String text, int position) {
		int index = text.indexOf('\n', position);
		return index < 0 ? text.length() : index;
	}

	public static int lineOf(String text, int position) {
		int line = 0;
		int end = Math.min(position, text.length());
		for (int i = 0; i < end; i++) {
			if (text.charAt(i) == '\n') {
				line++;
			}
		}
		return line;
	}

	public static int lineCount(String text) {
		int count = 1;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				count++;
			}
		}
		return count;
	}

	public static int lineStartOf(String text, int line) {
		int position = 0;
		for (int i = 0; i < line; i++) {
			int next = text.indexOf('\n', position);
			if (next < 0) {
				return text.length();
			}
			position = next + 1;
		}
		return position;
	}

	public static String stripControl(String text, boolean allowNewlines) {
		StringBuilder out = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\r') {
				continue;
			}
			if (c == '\n' && allowNewlines || c == '\t' && allowNewlines || c >= ' ' && c != 127) {
				out.append(c);
			} else if (c == '\n' || c == '\t') {
				out.append(' ');
			}
		}
		return out.toString();
	}
}
