package com.nick22985.nbteditor.json;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class JsonException extends Exception {
	private final int cursor;

	public JsonException(String message, int cursor) {
		super(message + " at position " + cursor);
		this.cursor = cursor;
	}

	public int cursor() {
		return cursor;
	}
}
