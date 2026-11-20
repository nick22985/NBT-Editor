package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextSpan;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TextSliceUtil {
	private TextSliceUtil() {
	}

	public static Text slice(Text text, int start, int end) {
		List<TextSpan> out = new ArrayList<>();
		int position = 0;
		for (TextSpan span : text.flatten()) {
			int spanStart = position;
			int spanEnd = position + span.text().length();
			position = spanEnd;
			int from = Math.max(start, spanStart);
			int to = Math.min(end, spanEnd);
			if (from < to) {
				out.add(new TextSpan(span.text().substring(from - spanStart, to - spanStart), span.style()));
			}
			if (position >= end) {
				break;
			}
		}
		return Text.fromSpans(out);
	}

	public static int length(Text text) {
		int length = 0;
		for (TextSpan span : text.flatten()) {
			length += span.text().length();
		}
		return length;
	}

	public static List<Text> lines(Text text) {
		List<Text> lines = new ArrayList<>();
		List<TextSpan> current = new ArrayList<>();
		for (TextSpan span : text.flatten()) {
			String value = span.text();
			int start = 0;
			int newline;
			while ((newline = value.indexOf('\n', start)) >= 0) {
				if (newline > start) {
					current.add(new TextSpan(value.substring(start, newline), span.style()));
				}
				lines.add(Text.fromSpans(current));
				current = new ArrayList<>();
				start = newline + 1;
			}
			if (start < value.length()) {
				current.add(new TextSpan(value.substring(start), span.style()));
			}
		}
		lines.add(Text.fromSpans(current));
		return lines;
	}
}
