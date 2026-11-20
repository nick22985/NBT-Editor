package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.ui.text.TextBuffer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TextKeyUtil {
	private TextKeyUtil() {
	}

	public static boolean handleCommon(TextBuffer buffer, KeyInput input, UiHost host, boolean editable) {
		boolean shift = input.modifiers().shift();
		boolean word = input.modifiers().primary();
		String text = buffer.text();
		if (input.shortcut(Key.A)) {
			buffer.selectAll();
			return true;
		}
		if (input.shortcut(Key.C)) {
			if (buffer.hasSelection() && host != null) {
				host.setClipboard(buffer.selected());
			}
			return true;
		}
		if (input.shortcut(Key.X)) {
			if (buffer.hasSelection() && host != null) {
				host.setClipboard(buffer.selected());
				if (editable) {
					buffer.deleteSelection();
				}
			}
			return true;
		}
		if (input.shortcut(Key.V)) {
			if (editable && host != null) {
				buffer.insert(host.clipboard());
			}
			return true;
		}
		if (input.shortcut(Key.Z)) {
			if (editable) {
				buffer.undo();
			}
			return true;
		}
		if (input.shortcut(Key.Y) || input.shiftShortcut(Key.Z)) {
			if (editable) {
				buffer.redo();
			}
			return true;
		}
		switch (input.key()) {
			case Key.BACKSPACE:
				if (editable) {
					buffer.deleteBackward(word);
				}
				return true;
			case Key.DELETE:
				if (editable) {
					buffer.deleteForward(word);
				}
				return true;
			case Key.LEFT:
				if (buffer.hasSelection() && !shift && !word) {
					buffer.moveTo(buffer.selectionStart(), false);
				} else {
					buffer.moveTo(word ? TextEditUtil.wordLeft(text, buffer.cursor()) : buffer.cursor() - 1, shift);
				}
				return true;
			case Key.RIGHT:
				if (buffer.hasSelection() && !shift && !word) {
					buffer.moveTo(buffer.selectionEnd(), false);
				} else {
					buffer.moveTo(word ? TextEditUtil.wordRight(text, buffer.cursor()) : buffer.cursor() + 1, shift);
				}
				return true;
			case Key.HOME:
				buffer.moveTo(word || !buffer.multiline() ? 0 : TextEditUtil.lineStart(text, buffer.cursor()), shift);
				return true;
			case Key.END:
				buffer.moveTo(word || !buffer.multiline() ? text.length() : TextEditUtil.lineEnd(text, buffer.cursor()), shift);
				return true;
			default:
				return false;
		}
	}

	public static boolean isTypable(int codepoint) {
		return codepoint >= 32 && codepoint != 127 && Character.isValidCodePoint(codepoint) && !Character.isISOControl(codepoint);
	}
}
