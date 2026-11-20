package com.nick22985.nbteditor.hook;

import com.nick22985.nbteditor.ui.HoveredItem;
import com.nick22985.nbteditor.ui.KeyInput;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ScreenKeyEvent extends CancellableEvent {
	private final KeyInput input;
	private final HoveredItem hovered;
	private final boolean typing;
	private final boolean modScreen;

	public ScreenKeyEvent(KeyInput input, HoveredItem hovered, boolean typing, boolean modScreen) {
		this.input = input;
		this.hovered = hovered;
		this.typing = typing;
		this.modScreen = modScreen;
	}

	public KeyInput input() {
		return this.input;
	}

	public HoveredItem hovered() {
		return this.hovered;
	}

	public boolean typing() {
		return this.typing;
	}

	public boolean modScreen() {
		return this.modScreen;
	}
}
