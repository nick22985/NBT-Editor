package com.nick22985.nbteditor.nbt;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SnbtException extends Exception {
	private final int cursor;
	private final String input;

	public SnbtException(String message, String input, int cursor) {
		super(message);
		this.input = input;
		this.cursor = cursor;
	}

	public int cursor() {
		return cursor;
	}

	public String input() {
		return input;
	}

	public String context() {
		int start = Math.max(0, cursor - 20);
		int end = Math.min(input.length(), cursor);
		return (start > 0 ? "..." : "") + input.substring(start, end) + "<--[HERE]";
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " at position " + cursor + ": " + context();
	}
}
