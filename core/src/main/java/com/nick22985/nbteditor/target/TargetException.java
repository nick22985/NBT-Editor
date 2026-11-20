package com.nick22985.nbteditor.target;

import com.nick22985.nbteditor.text.Text;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class TargetException extends RuntimeException {
	private final Text reason;

	public TargetException(Text reason) {
		super(reason.plain());
		this.reason = reason;
	}

	public TargetException(Text reason, Throwable cause) {
		super(reason.plain(), cause);
		this.reason = reason;
	}

	public Text reason() {
		return this.reason;
	}
}
