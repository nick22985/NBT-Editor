package com.nick22985.nbteditor.ui.text;

import com.nick22985.nbteditor.util.TextEditUtil;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TextBuffer {
	private static final int HISTORY = 200;
	private static final long COALESCE_MILLIS = 600;

	private final boolean multiline;
	private final Deque<TextSnapshot> undo = new ArrayDeque<>();
	private final Deque<TextSnapshot> redo = new ArrayDeque<>();
	private String text = "";
	private int cursor;
	private int anchor;
	private int maxLength = Integer.MAX_VALUE;
	private Predicate<String> filter = value -> true;
	private Consumer<String> listener = value -> {
	};
	private long lastTyped;

	public TextBuffer(boolean multiline) {
		this.multiline = multiline;
	}

	public boolean multiline() {
		return this.multiline;
	}

	public String text() {
		return this.text;
	}

	public int cursor() {
		return this.cursor;
	}

	public int anchor() {
		return this.anchor;
	}

	public int selectionStart() {
		return Math.min(this.cursor, this.anchor);
	}

	public int selectionEnd() {
		return Math.max(this.cursor, this.anchor);
	}

	public boolean hasSelection() {
		return this.cursor != this.anchor;
	}

	public String selected() {
		return this.text.substring(this.selectionStart(), this.selectionEnd());
	}

	public TextBuffer maxLength(int maxLength) {
		this.maxLength = maxLength;
		if (this.text.length() > maxLength) {
			this.set(this.text.substring(0, maxLength));
		}
		return this;
	}

	public int maxLength() {
		return this.maxLength;
	}

	public TextBuffer filter(Predicate<String> filter) {
		this.filter = filter;
		return this;
	}

	public TextBuffer listener(Consumer<String> listener) {
		this.listener = listener;
		return this;
	}

	public void set(String value) {
		String clean = TextEditUtil.stripControl(value, this.multiline);
		if (clean.length() > this.maxLength) {
			clean = clean.substring(0, this.maxLength);
		}
		if (clean.equals(this.text)) {
			return;
		}
		this.text = clean;
		this.cursor = Math.min(this.cursor, clean.length());
		this.anchor = Math.min(this.anchor, clean.length());
		this.listener.accept(clean);
	}

	public void reset(String value) {
		this.undo.clear();
		this.redo.clear();
		this.set(value);
		this.moveTo(this.text.length(), false);
	}

	public void moveTo(int position, boolean select) {
		this.cursor = Math.max(0, Math.min(position, this.text.length()));
		if (!select) {
			this.anchor = this.cursor;
		}
	}

	public void select(int start, int end) {
		this.anchor = Math.max(0, Math.min(start, this.text.length()));
		this.cursor = Math.max(0, Math.min(end, this.text.length()));
	}

	public void selectAll() {
		this.select(0, this.text.length());
	}

	private TextSnapshot snapshot() {
		return new TextSnapshot(this.text, this.cursor, this.anchor);
	}

	private void remember(boolean typing) {
		long now = System.currentTimeMillis();
		if (typing && now - this.lastTyped < COALESCE_MILLIS && !this.undo.isEmpty()) {
			this.lastTyped = now;
			return;
		}
		this.lastTyped = typing ? now : 0;
		this.undo.push(this.snapshot());
		while (this.undo.size() > HISTORY) {
			this.undo.removeLast();
		}
		this.redo.clear();
	}

	private boolean apply(String candidate, int newCursor, boolean typing) {
		if (candidate.length() > this.maxLength || !this.filter.test(candidate)) {
			return false;
		}
		this.remember(typing);
		this.text = candidate;
		this.cursor = newCursor;
		this.anchor = newCursor;
		this.listener.accept(candidate);
		return true;
	}

	public boolean insert(String value) {
		return this.insert(value, false);
	}

	public boolean type(String value) {
		return this.insert(value, true);
	}

	private boolean insert(String value, boolean typing) {
		String clean = TextEditUtil.stripControl(value, this.multiline);
		int start = this.selectionStart();
		int end = this.selectionEnd();
		int room = this.maxLength - (this.text.length() - (end - start));
		if (room <= 0 && !clean.isEmpty()) {
			return false;
		}
		if (clean.length() > room) {
			clean = clean.substring(0, room);
		}
		String candidate = this.text.substring(0, start) + clean + this.text.substring(end);
		return this.apply(candidate, start + clean.length(), typing && !this.hasSelection());
	}

	public boolean deleteSelection() {
		if (!this.hasSelection()) {
			return false;
		}
		return this.insert("");
	}

	public boolean deleteBackward(boolean word) {
		if (this.hasSelection()) {
			return this.deleteSelection();
		}
		if (this.cursor == 0) {
			return false;
		}
		int from = word ? TextEditUtil.wordLeft(this.text, this.cursor) : this.cursor - 1;
		return this.apply(this.text.substring(0, from) + this.text.substring(this.cursor), from, !word);
	}

	public boolean deleteForward(boolean word) {
		if (this.hasSelection()) {
			return this.deleteSelection();
		}
		if (this.cursor >= this.text.length()) {
			return false;
		}
		int to = word ? TextEditUtil.wordRight(this.text, this.cursor) : this.cursor + 1;
		return this.apply(this.text.substring(0, this.cursor) + this.text.substring(to), this.cursor, !word);
	}

	public boolean undo() {
		if (this.undo.isEmpty()) {
			return false;
		}
		this.redo.push(this.snapshot());
		this.restore(this.undo.pop());
		return true;
	}

	public boolean redo() {
		if (this.redo.isEmpty()) {
			return false;
		}
		this.undo.push(this.snapshot());
		this.restore(this.redo.pop());
		return true;
	}

	private void restore(TextSnapshot snapshot) {
		this.text = snapshot.text();
		this.cursor = Math.min(snapshot.cursor(), this.text.length());
		this.anchor = Math.min(snapshot.anchor(), this.text.length());
		this.lastTyped = 0;
		this.listener.accept(this.text);
	}
}
