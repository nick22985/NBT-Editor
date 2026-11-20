package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.ui.text.TextMatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TextSearchUtil {
	private TextSearchUtil() {
	}

	public static Pattern pattern(String query, boolean regex) {
		return regex ? Pattern.compile(query, Pattern.MULTILINE) : Pattern.compile(Pattern.quote(query));
	}

	public static TextMatch next(Pattern pattern, String text, int from) {
		int start = Math.max(0, Math.min(from, text.length()));
		TextMatch match = find(pattern, text, start, text.length() + 1);
		return match != null ? match : find(pattern, text, 0, start);
	}

	public static String expand(Pattern pattern, String text, TextMatch match, String replacement, boolean regex) {
		Matcher matcher = pattern.matcher(text);
		if (!matcher.find(match.start()) || matcher.start() != match.start() || matcher.end() != match.end()) {
			return null;
		}
		StringBuilder out = new StringBuilder();
		matcher.appendReplacement(out, regex ? replacement : Matcher.quoteReplacement(replacement));
		return out.substring(match.start());
	}

	public static String replaceAll(Pattern pattern, String text, String replacement, boolean regex) {
		return pattern.matcher(text).replaceAll(regex ? replacement : Matcher.quoteReplacement(replacement));
	}

	private static TextMatch find(Pattern pattern, String text, int from, int limit) {
		Matcher matcher = pattern.matcher(text);
		int start = from;
		while (start <= text.length() && matcher.find(start)) {
			if (matcher.start() >= limit) {
				return null;
			}
			if (matcher.end() > matcher.start()) {
				return new TextMatch(matcher.start(), matcher.end());
			}
			start = matcher.end() + 1;
		}
		return null;
	}
}
