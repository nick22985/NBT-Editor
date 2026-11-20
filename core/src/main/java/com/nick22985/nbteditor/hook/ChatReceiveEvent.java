package com.nick22985.nbteditor.hook;

import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ChatReceiveEvent extends CancellableEvent {
	private final Text message;
	private final boolean overlay;

	public ChatReceiveEvent(Text message, boolean overlay) {
		this.message = message;
		this.overlay = overlay;
	}

	public Text message() {
		return this.message;
	}

	public boolean overlay() {
		return this.overlay;
	}
}
