package com.nick22985.nbteditor.ui.suggest;

import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record TextSuggestion(int start, int end, String text, Text tooltip) {
	public TextSuggestion shift(int amount) {
		return new TextSuggestion(this.start + amount, this.end + amount, this.text, this.tooltip);
	}
}
