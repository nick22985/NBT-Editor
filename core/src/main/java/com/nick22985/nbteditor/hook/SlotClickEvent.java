package com.nick22985.nbteditor.hook;

import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.ui.HoveredItem;
import com.nick22985.nbteditor.ui.Modifiers;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SlotClickEvent extends CancellableEvent {
	private final HoveredItem slot;
	private final GameItem cursor;
	private final Modifiers modifiers;

	public SlotClickEvent(HoveredItem slot, GameItem cursor, Modifiers modifiers) {
		this.slot = slot;
		this.cursor = cursor;
		this.modifiers = modifiers;
	}

	public HoveredItem slot() {
		return this.slot;
	}

	public GameItem cursor() {
		return this.cursor;
	}

	public Modifiers modifiers() {
		return this.modifiers;
	}
}
