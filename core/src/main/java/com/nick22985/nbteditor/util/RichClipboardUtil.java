package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class RichClipboardUtil {
	private static Text copied;
	private static String copiedPlain;

	private RichClipboardUtil() {
	}

	public static void remember(Text text, String clipboard) {
		copied = text;
		copiedPlain = clipboard;
	}

	public static Text recall(String clipboard) {
		if (copied != null && clipboard != null && clipboard.equals(copiedPlain)) {
			return copied;
		}
		return null;
	}
}
