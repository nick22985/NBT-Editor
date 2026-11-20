package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record TabSpec(String id, Text label, boolean available, Text unavailableReason) {
	public static TabSpec of(String id, Text label) {
		return new TabSpec(id, label, true, null);
	}

	public TabSpec unavailable(Text reason) {
		return new TabSpec(this.id, this.label, false, reason);
	}
}
