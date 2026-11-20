package com.nick22985.nbteditor.command;

import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CommandException extends Exception {
	private static final long serialVersionUID = 1L;

	private final transient Text text;

	public CommandException(Text text) {
		super(text.plain());
		this.text = text;
	}

	public Text text() {
		return this.text;
	}
}
