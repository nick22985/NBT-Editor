package com.nick22985.nbteditor.ui.tab;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum TabPosition {
	HIDDEN(false, 0),
	TOP_LEFT(true, -1),
	TOP_CENTER(true, 0),
	TOP_RIGHT(true, 1),
	BOTTOM_LEFT(false, -1),
	BOTTOM_CENTER(false, 0),
	BOTTOM_RIGHT(false, 1);

	private final boolean top;
	private final int align;

	TabPosition(boolean top, int align) {
		this.top = top;
		this.align = align;
	}

	public boolean top() {
		return this.top;
	}

	public int align() {
		return this.align;
	}

	public boolean hidden() {
		return this == HIDDEN;
	}
}
