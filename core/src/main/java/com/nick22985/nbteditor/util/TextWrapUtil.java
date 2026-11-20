package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.TextMetrics;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TextWrapUtil {
	private TextWrapUtil() {
	}

	public static List<Text> wrap(TextMetrics metrics, Text text, int width) {
		List<Text> out = new ArrayList<>();
		for (Text line : TextSliceUtil.lines(text)) {
			String plain = line.plain();
			int length = plain.length();
			if (length == 0) {
				out.add(Text.EMPTY);
				continue;
			}
			int start = 0;
			while (start < length) {
				int end = fit(metrics, line, start, length, width);
				int next = end;
				if (end < length) {
					int space = plain.lastIndexOf(' ', end);
					if (space > start) {
						end = space;
						next = space + 1;
					} else if (plain.charAt(end) == ' ') {
						next = end + 1;
					}
				}
				out.add(TextSliceUtil.slice(line, start, end));
				start = next;
			}
		}
		if (out.isEmpty()) {
			out.add(Text.EMPTY);
		}
		return out;
	}

	public static String trim(TextMetrics metrics, String text, int width) {
		if (metrics.textWidth(text) <= width) {
			return text;
		}
		int low = 0;
		int high = text.length();
		while (low < high) {
			int middle = (low + high + 1) / 2;
			if (metrics.textWidth(text.substring(0, middle)) <= width) {
				low = middle;
			} else {
				high = middle - 1;
			}
		}
		return text.substring(0, low);
	}

	public static Text trim(TextMetrics metrics, Text text, int width) {
		if (metrics.textWidth(text) <= width) {
			return text;
		}
		int length = TextSliceUtil.length(text);
		int low = 0;
		int high = length;
		while (low < high) {
			int middle = (low + high + 1) / 2;
			if (metrics.textWidth(TextSliceUtil.slice(text, 0, middle)) <= width) {
				low = middle;
			} else {
				high = middle - 1;
			}
		}
		return TextSliceUtil.slice(text, 0, low);
	}

	private static int fit(TextMetrics metrics, Text line, int start, int length, int width) {
		int low = start + 1;
		int high = length;
		while (low < high) {
			int middle = (low + high + 1) / 2;
			if (metrics.textWidth(TextSliceUtil.slice(line, start, middle)) <= width) {
				low = middle;
			} else {
				high = middle - 1;
			}
		}
		return low;
	}
}
