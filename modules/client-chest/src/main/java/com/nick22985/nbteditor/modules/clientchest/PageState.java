package com.nick22985.nbteditor.modules.clientchest;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public enum PageState {
	OK(true),
	UPGRADED(true),
	NEWER(false),
	BROKEN(false);

	private final boolean writable;

	PageState(boolean writable) {
		this.writable = writable;
	}

	public boolean writable() {
		return this.writable;
	}
}
