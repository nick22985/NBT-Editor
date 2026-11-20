package com.nick22985.nbteditor.ui.text;

import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.RichTextUtil;
import com.nick22985.nbteditor.util.TextEditUtil;
import com.nick22985.nbteditor.util.TextKeyUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class RichBuffer {
	private static final int HISTORY = 200;
	private static final long COALESCE_MILLIS = 600;

	private final boolean multiline;
	private final Deque<RichSnapshot> undo = new ArrayDeque<>();
	private final Deque<RichSnapshot> redo = new ArrayDeque<>();
	private List<RichCell> cells = new ArrayList<>();
	private String plain = "";
	private int cursor;
	private int anchor;
	private int maxLength = Integer.MAX_VALUE;
	private Style pending;
	private Runnable listener = () -> {
	};
	private long lastTyped;
	private int revision;

	public RichBuffer(boolean multiline) {
		this.multiline = multiline;
	}

	public boolean multiline() {
		return this.multiline;
	}

	public List<RichCell> cells() {
		return this.cells;
	}

	public String text() {
		return this.plain;
	}

	public int length() {
		return this.cells.size();
	}

	public int revision() {
		return this.revision;
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

	public RichBuffer maxLength(int maxLength) {
		this.maxLength = maxLength;
		return this;
	}

	public RichBuffer listener(Runnable listener) {
		this.listener = listener;
		return this;
	}

	public Text toText() {
		return RichTextUtil.build(this.cells);
	}

	public List<Text> lines() {
		return RichTextUtil.lines(this.cells);
	}

	public Text selectedText() {
		return RichTextUtil.build(this.cells, this.selectionStart(), this.selectionEnd());
	}

	public String selectedPlain() {
		return this.plain.substring(this.selectionStart(), this.selectionEnd());
	}

	public void reset(List<RichCell> value) {
		this.undo.clear();
		this.redo.clear();
		this.replace(this.clean(value));
		this.cursor = this.cells.size();
		this.anchor = this.cursor;
		this.pending = null;
	}

	public void reset(Text value) {
		this.reset(RichTextUtil.cells(value));
	}

	public void resetLines(List<Text> lines) {
		this.reset(RichTextUtil.cells(lines));
	}

	private List<RichCell> clean(List<RichCell> value) {
		List<RichCell> out = new ArrayList<>(value.size());
		for (RichCell cell : value) {
			if (cell.isAtom() || cell.ch() == '\n' && this.multiline || TextKeyUtil.isTypable(cell.ch()) || Character.isSurrogate(cell.ch())) {
				out.add(cell);
			}
			if (out.size() >= this.maxLength) {
				break;
			}
		}
		return out;
	}

	private void replace(List<RichCell> next) {
		this.cells = next;
		this.plain = RichTextUtil.plain(next);
		this.revision++;
		this.listener.run();
	}

	public void moveTo(int position, boolean select) {
		this.cursor = Math.max(0, Math.min(position, this.cells.size()));
		if (!select) {
			this.anchor = this.cursor;
		}
		this.pending = null;
	}

	public void select(int start, int end) {
		this.anchor = Math.max(0, Math.min(start, this.cells.size()));
		this.cursor = Math.max(0, Math.min(end, this.cells.size()));
		this.pending = null;
	}

	public void selectAll() {
		this.select(0, this.cells.size());
	}

	public Style styleAt(int index) {
		if (this.cells.isEmpty()) {
			return Style.EMPTY;
		}
		int at = index > 0 ? index - 1 : 0;
		at = Math.min(at, this.cells.size() - 1);
		RichCell cell = this.cells.get(at);
		if (cell.isNewline() && index < this.cells.size() && index > 0) {
			cell = this.cells.get(index);
		}
		return cell.style();
	}

	public Style typingStyle() {
		if (this.pending != null) {
			return this.pending;
		}
		return this.styleAt(this.hasSelection() ? this.selectionStart() + 1 : this.cursor);
	}

	public Style selectionStyle() {
		if (this.hasSelection()) {
			return this.cells.get(this.selectionStart()).style();
		}
		return this.typingStyle();
	}

	private RichSnapshot snapshot() {
		return new RichSnapshot(this.cells, this.cursor, this.anchor);
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

	private boolean splice(int start, int end, List<RichCell> insert, boolean typing) {
		List<RichCell> clean = this.clean(insert);
		int room = this.maxLength - (this.cells.size() - (end - start));
		if (room <= 0 && !clean.isEmpty()) {
			return false;
		}
		if (clean.size() > room) {
			clean = clean.subList(0, room);
		}
		if (clean.isEmpty() && start == end) {
			return false;
		}
		this.remember(typing);
		List<RichCell> next = new ArrayList<>(this.cells.size() - (end - start) + clean.size());
		next.addAll(this.cells.subList(0, start));
		next.addAll(clean);
		next.addAll(this.cells.subList(end, this.cells.size()));
		int position = start + clean.size();
		this.cursor = position;
		this.anchor = position;
		this.replace(next);
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
		Style style = this.typingStyle();
		List<RichCell> insert = new ArrayList<>(clean.length());
		for (int i = 0; i < clean.length(); i++) {
			char c = clean.charAt(i);
			insert.add(RichCell.of(c, c == '\n' ? Style.EMPTY : style));
		}
		boolean selection = this.hasSelection();
		Style keep = this.pending;
		boolean changed = this.splice(this.selectionStart(), this.selectionEnd(), insert, typing && !selection);
		if (changed && keep != null && !clean.isEmpty()) {
			this.pending = null;
		}
		return changed;
	}

	public boolean insert(List<RichCell> value) {
		return this.splice(this.selectionStart(), this.selectionEnd(), value, false);
	}

	public boolean insert(Text value) {
		return this.insert(RichTextUtil.cells(value));
	}

	public boolean deleteSelection() {
		if (!this.hasSelection()) {
			return false;
		}
		return this.splice(this.selectionStart(), this.selectionEnd(), List.of(), false);
	}

	public boolean deleteBackward(boolean word) {
		if (this.hasSelection()) {
			return this.deleteSelection();
		}
		if (this.cursor == 0) {
			return false;
		}
		int from = word ? TextEditUtil.wordLeft(this.plain, this.cursor) : this.cursor - 1;
		return this.splice(from, this.cursor, List.of(), !word);
	}

	public boolean deleteForward(boolean word) {
		if (this.hasSelection()) {
			return this.deleteSelection();
		}
		if (this.cursor >= this.cells.size()) {
			return false;
		}
		int to = word ? TextEditUtil.wordRight(this.plain, this.cursor) : this.cursor + 1;
		return this.splice(this.cursor, to, List.of(), !word);
	}

	public void style(UnaryOperator<Style> change) {
		if (!this.hasSelection()) {
			this.pending = change.apply(this.typingStyle());
			return;
		}
		int start = this.selectionStart();
		int end = this.selectionEnd();
		List<RichCell> next = new ArrayList<>(this.cells);
		boolean changed = false;
		for (int i = start; i < end; i++) {
			RichCell cell = next.get(i);
			if (cell.isNewline()) {
				continue;
			}
			Style updated = change.apply(cell.style());
			if (!updated.equals(cell.style())) {
				next.set(i, cell.withStyle(updated));
				changed = true;
			}
		}
		if (!changed) {
			return;
		}
		this.remember(false);
		int keepCursor = this.cursor;
		int keepAnchor = this.anchor;
		this.replace(next);
		this.cursor = keepCursor;
		this.anchor = keepAnchor;
	}

	public void restyle(int start, int end, List<Style> styles) {
		List<RichCell> next = new ArrayList<>(this.cells);
		for (int i = start; i < end && i - start < styles.size(); i++) {
			RichCell cell = next.get(i);
			if (!cell.isNewline()) {
				next.set(i, cell.withStyle(styles.get(i - start)));
			}
		}
		this.remember(false);
		int keepCursor = this.cursor;
		int keepAnchor = this.anchor;
		this.replace(next);
		this.cursor = keepCursor;
		this.anchor = keepAnchor;
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

	private void restore(RichSnapshot snapshot) {
		this.lastTyped = 0;
		this.pending = null;
		this.cursor = Math.min(snapshot.cursor(), snapshot.cells().size());
		this.anchor = Math.min(snapshot.anchor(), snapshot.cells().size());
		this.replace(snapshot.cells());
	}
}
