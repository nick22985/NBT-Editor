package com.nick22985.nbteditor.ui.text;

import com.nick22985.nbteditor.text.Content;
import com.nick22985.nbteditor.text.Style;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record RichCell(char ch, Style style, Content atom) {
	public static final char ATOM = '￼';

	public static RichCell of(char ch, Style style) {
		return new RichCell(ch, style, null);
	}

	public static RichCell atom(Content atom, Style style) {
		return new RichCell(ATOM, style, atom);
	}

	public boolean isAtom() {
		return this.atom != null;
	}

	public boolean isNewline() {
		return this.atom == null && this.ch == '\n';
	}

	public RichCell withStyle(Style style) {
		return new RichCell(this.ch, style, this.atom);
	}
}
