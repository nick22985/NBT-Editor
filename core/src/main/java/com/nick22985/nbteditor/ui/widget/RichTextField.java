package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.LiteralContent;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextColor;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.Modifiers;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.TextMetrics;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.text.RichBuffer;
import com.nick22985.nbteditor.ui.text.RichCell;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.RichClipboardUtil;
import com.nick22985.nbteditor.util.RichTextUtil;
import com.nick22985.nbteditor.util.TextEditUtil;
import com.nick22985.nbteditor.util.TextKeyUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class RichTextField extends Widget {
	private static final int INSET = 4;

	private final RichBuffer buffer;
	private final boolean multiline;
	private Style defaults = Style.EMPTY;
	private Text placeholder;
	private boolean editable = true;
	private Runnable onChange = () -> {
	};
	private Runnable onCaret = () -> {
	};
	private Function<String, Text> pasteParser;
	private Function<Text, String> copyFormatter;
	private int cachedRevision = -1;
	private Style cachedDefaults;
	private List<RichCell> display = List.of();
	private int[] offsets = {0};
	private int[] lineStarts = {0};
	private int[] lineEnds = {0};
	private int scrollY;
	private int scrollX;
	private int preferredPx = -1;
	private boolean dragging;
	private long focusTime;
	private int lastCursor = -1;
	private int lastAnchor = -1;

	public RichTextField(boolean multiline) {
		this.multiline = multiline;
		this.buffer = new RichBuffer(multiline);
		this.buffer.listener(this::changed);
		this.height = Theme.FIELD_HEIGHT;
	}

	private void changed() {
		this.onChange.run();
	}

	public RichBuffer buffer() {
		return this.buffer;
	}

	public boolean multiline() {
		return this.multiline;
	}

	public Text value() {
		return this.buffer.toText();
	}

	public List<Text> lines() {
		return this.buffer.lines();
	}

	public RichTextField value(Text value) {
		Runnable keep = this.onChange;
		this.onChange = () -> {
		};
		this.buffer.reset(value);
		this.onChange = keep;
		this.resetScroll();
		return this;
	}

	public RichTextField lines(List<Text> lines) {
		Runnable keep = this.onChange;
		this.onChange = () -> {
		};
		this.buffer.resetLines(lines);
		this.onChange = keep;
		this.resetScroll();
		return this;
	}

	private void resetScroll() {
		if (!this.focused()) {
			this.buffer.moveTo(0, false);
		}
		this.scrollX = 0;
		this.scrollY = 0;
	}

	public RichTextField defaults(Style defaults) {
		this.defaults = defaults == null ? Style.EMPTY : defaults;
		return this;
	}

	public Style defaults() {
		return this.defaults;
	}

	public RichTextField placeholder(Text placeholder) {
		this.placeholder = placeholder;
		return this;
	}

	public RichTextField editable(boolean editable) {
		this.editable = editable;
		return this;
	}

	public boolean editable() {
		return this.editable;
	}

	public RichTextField maxLength(int maxLength) {
		this.buffer.maxLength(maxLength);
		return this;
	}

	public RichTextField onChange(Runnable onChange) {
		this.onChange = onChange;
		return this;
	}

	public RichTextField onCaret(Runnable onCaret) {
		this.onCaret = onCaret;
		return this;
	}

	public RichTextField pasteParser(Function<String, Text> pasteParser) {
		this.pasteParser = pasteParser;
		return this;
	}

	public RichTextField copyFormatter(Function<Text, String> copyFormatter) {
		this.copyFormatter = copyFormatter;
		return this;
	}

	public Style resolved(Style style) {
		return style.inheritFrom(this.defaults);
	}

	public Style currentStyle() {
		return this.resolved(this.buffer.selectionStyle());
	}

	public boolean active(Formatting formatting) {
		Style style = this.currentStyle();
		switch (formatting) {
			case BOLD:
				return style.isBold();
			case ITALIC:
				return style.isItalic();
			case UNDERLINE:
				return style.isUnderlined();
			case STRIKETHROUGH:
				return style.isStrikethrough();
			case OBFUSCATED:
				return style.isObfuscated();
			default:
				return false;
		}
	}

	public void toggle(Formatting formatting) {
		boolean on = !this.active(formatting);
		Boolean value = on ? Boolean.TRUE : this.defaultOn(formatting) ? Boolean.FALSE : null;
		this.style(style -> this.set(style, formatting, value));
	}

	private boolean defaultOn(Formatting formatting) {
		switch (formatting) {
			case BOLD:
				return this.defaults.isBold();
			case ITALIC:
				return this.defaults.isItalic();
			case UNDERLINE:
				return this.defaults.isUnderlined();
			case STRIKETHROUGH:
				return this.defaults.isStrikethrough();
			case OBFUSCATED:
				return this.defaults.isObfuscated();
			default:
				return false;
		}
	}

	private Style set(Style style, Formatting formatting, Boolean value) {
		switch (formatting) {
			case BOLD:
				return style.withBold(value);
			case ITALIC:
				return style.withItalic(value);
			case UNDERLINE:
				return style.withUnderlined(value);
			case STRIKETHROUGH:
				return style.withStrikethrough(value);
			case OBFUSCATED:
				return style.withObfuscated(value);
			default:
				return style;
		}
	}

	public void color(TextColor color) {
		this.style(style -> style.withColor(color));
	}

	public void style(UnaryOperator<Style> change) {
		if (!this.editable) {
			return;
		}
		this.buffer.style(change);
		this.onCaret.run();
	}

	public void insert(Text text) {
		if (this.editable) {
			this.buffer.insert(text);
		}
	}

	@Override
	public boolean focusable() {
		return true;
	}

	@Override
	protected void focusChanged(boolean focused) {
		UiHost host = this.host();
		this.focusTime = host == null ? 0 : host.millis();
	}

	private Text cellText(RichCell cell) {
		return Text.of(LiteralContent.EMPTY, this.defaults, List.of(RichTextUtil.display(cell)));
	}

	private void layout(TextMetrics metrics) {
		if (this.cachedRevision == this.buffer.revision() && this.defaults.equals(this.cachedDefaults)) {
			return;
		}
		this.cachedRevision = this.buffer.revision();
		this.cachedDefaults = this.defaults;
		List<RichCell> cells = this.buffer.cells();
		List<RichCell> shown = new ArrayList<>(cells.size());
		this.offsets = new int[cells.size() + 1];
		List<Integer> starts = new ArrayList<>();
		List<Integer> ends = new ArrayList<>();
		starts.add(0);
		int px = 0;
		for (int i = 0; i < cells.size(); i++) {
			RichCell cell = cells.get(i);
			this.offsets[i] = px;
			if (cell.isNewline()) {
				shown.add(cell);
				ends.add(i);
				starts.add(i + 1);
				px = 0;
				continue;
			}
			RichCell visible = cell;
			int width = metrics.textWidth(this.cellText(cell));
			if (cell.isAtom() && width <= 0) {
				visible = RichCell.atom(new LiteralContent(RichTextUtil.placeholder(cell).plain()), cell.style());
				width = metrics.textWidth(this.cellText(visible));
			}
			shown.add(visible);
			px += width;
		}
		this.offsets[cells.size()] = px;
		ends.add(cells.size());
		this.display = shown;
		this.lineStarts = starts.stream().mapToInt(Integer::intValue).toArray();
		this.lineEnds = ends.stream().mapToInt(Integer::intValue).toArray();
	}

	private int lineOf(int index) {
		int low = 0;
		int high = this.lineStarts.length - 1;
		while (low < high) {
			int mid = (low + high + 1) / 2;
			if (this.lineStarts[mid] <= index) {
				low = mid;
			} else {
				high = mid - 1;
			}
		}
		return low;
	}

	private int px(int index) {
		return this.offsets[Math.max(0, Math.min(index, this.offsets.length - 1))];
	}

	private int lineWidth(int line) {
		return this.px(this.lineEnds[line]);
	}

	private int textLeft() {
		return this.x + INSET;
	}

	private int textTop() {
		return this.multiline ? this.y + INSET : this.y + (this.height - 8) / 2;
	}

	private int available() {
		return this.width - INSET * 2 - (this.multiline ? Theme.SCROLLBAR_WIDTH + 1 : 0);
	}

	private int visibleLines() {
		return Math.max(1, (this.height - INSET * 2) / Theme.LINE);
	}

	private int contentHeight() {
		return this.lineStarts.length * Theme.LINE + INSET * 2;
	}

	private void clampScroll() {
		this.scrollY = this.multiline ? DrawUtil.clamp(this.scrollY, 0, Math.max(0, this.contentHeight() - this.height)) : 0;
		int widest = 0;
		for (int line = 0; line < this.lineStarts.length; line++) {
			widest = Math.max(widest, this.lineWidth(line));
		}
		this.scrollX = DrawUtil.clamp(this.scrollX, 0, Math.max(0, widest - this.available() + 1));
	}

	private void followCursor() {
		int cursor = this.buffer.cursor();
		if (cursor == this.lastCursor && this.buffer.anchor() == this.lastAnchor) {
			return;
		}
		boolean moved = cursor != this.lastCursor;
		this.lastCursor = cursor;
		this.lastAnchor = this.buffer.anchor();
		this.onCaret.run();
		if (!moved) {
			return;
		}
		int line = this.lineOf(cursor);
		if (this.multiline) {
			int top = line * Theme.LINE;
			int view = this.height - INSET * 2;
			if (top < this.scrollY) {
				this.scrollY = top;
			} else if (top + Theme.LINE > this.scrollY + view) {
				this.scrollY = top + Theme.LINE - view;
			}
		}
		int px = this.px(cursor);
		int available = this.available();
		if (px - this.scrollX > available - 6) {
			this.scrollX = px - available + 6;
		} else if (px - this.scrollX < 0) {
			this.scrollX = Math.max(0, px - 20);
		}
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		this.layout(canvas);
		boolean focused = this.focused();
		if (focused) {
			this.followCursor();
		}
		this.clampScroll();
		int border = !this.editable || !this.interactive() ? Theme.FIELD_BORDER_READ_ONLY : focused ? Theme.FIELD_BORDER_FOCUSED : Theme.FIELD_BORDER;
		DrawUtil.field(canvas, this.x, this.y, this.width, this.height, border);
		int left = this.textLeft();
		int top = this.textTop();
		int available = this.available();
		canvas.pushClip(left - 1, this.y + 1, available + 2, this.height - 2);
		if (this.buffer.length() == 0 && this.placeholder != null && !focused) {
			DrawUtil.textClipped(canvas, this.placeholder, left, top, available, Theme.FIELD_PLACEHOLDER);
		}
		int first = this.scrollY / Theme.LINE;
		int last = Math.min(this.lineStarts.length - 1, (this.scrollY + this.height) / Theme.LINE);
		int selectionStart = this.buffer.selectionStart();
		int selectionEnd = this.buffer.selectionEnd();
		boolean selection = focused && this.buffer.hasSelection();
		for (int line = first; line <= last; line++) {
			int lineY = top + line * Theme.LINE - this.scrollY;
			int start = this.lineStarts[line];
			int end = this.lineEnds[line];
			if (selection && selectionStart <= end && selectionEnd >= start) {
				int fromPx = this.px(Math.max(selectionStart, start));
				int toPx = this.px(Math.min(selectionEnd, end)) + (selectionEnd > end ? 4 : 0);
				DrawUtil.rect(canvas, left + fromPx - this.scrollX, lineY - 1, Math.max(1, toPx - fromPx), Theme.LINE, Theme.FIELD_SELECTION);
			}
			if (end > start) {
				Text text = Text.of(LiteralContent.EMPTY, this.defaults, List.of(RichTextUtil.build(this.display, start, end)));
				canvas.text(text, left - this.scrollX, lineY, this.editable && this.interactive() ? Theme.FIELD_TEXT : Theme.LABEL, true);
			}
		}
		UiHost host = this.host();
		long millis = host == null ? 0 : host.millis();
		if (focused && this.editable && (millis - this.focusTime) / 500 % 2 == 0) {
			int cursor = this.buffer.cursor();
			int line = this.lineOf(cursor);
			int caretX = left + this.px(cursor) - this.scrollX;
			DrawUtil.rect(canvas, caretX, top + line * Theme.LINE - this.scrollY - 1, 1, Theme.LINE, 0xFFD0D0D0);
		}
		canvas.popClip();
		if (this.multiline) {
			DrawUtil.scrollbar(canvas, this.x + this.width - 1 - Theme.SCROLLBAR_WIDTH, this.y + 1, this.height - 2, this.contentHeight(), this.scrollY);
		}
	}

	private int columnAt(int line, double px) {
		int start = this.lineStarts[line];
		int end = this.lineEnds[line];
		for (int i = start; i < end; i++) {
			int from = this.px(i);
			int to = this.px(i + 1);
			if (px < (from + to) / 2.0) {
				return i;
			}
		}
		return end;
	}

	private int indexAt(double mouseX, double mouseY) {
		UiHost host = this.host();
		if (host != null) {
			this.layout(host);
		}
		int line = this.multiline ? DrawUtil.clamp((int) Math.floor((mouseY - this.y - INSET + this.scrollY) / Theme.LINE), 0, this.lineStarts.length - 1) : 0;
		return this.columnAt(line, mouseX - this.textLeft() + this.scrollX);
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (!input.left() || !this.contains(input.x(), input.y())) {
			return false;
		}
		int index = this.indexAt(input.x(), input.y());
		if (input.doubleClick()) {
			int[] word = TextEditUtil.wordAt(this.buffer.text(), index);
			this.buffer.select(word[0], word[1]);
		} else {
			this.buffer.moveTo(index, input.modifiers().shift());
		}
		this.preferredPx = -1;
		this.dragging = true;
		return true;
	}

	@Override
	public boolean mouseDragged(MouseInput input, double deltaX, double deltaY) {
		if (!this.dragging) {
			return false;
		}
		this.buffer.moveTo(this.indexAt(input.x(), input.y()), true);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseInput input) {
		this.dragging = false;
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		int before = this.scrollY;
		int beforeX = this.scrollX;
		UiHost host = this.host();
		if (!this.multiline || host != null && host.modifiers().shift()) {
			this.scrollX -= (int) Math.round((deltaY == 0 ? deltaX : deltaY) * 20);
		} else {
			this.scrollY -= (int) Math.round(deltaY * Theme.LINE * 3);
			this.scrollX -= (int) Math.round(deltaX * 20);
		}
		this.clampScroll();
		return before != this.scrollY || beforeX != this.scrollX;
	}

	private void moveVertical(int lines, boolean select) {
		UiHost host = this.host();
		if (host != null) {
			this.layout(host);
		}
		int cursor = this.buffer.cursor();
		int line = this.lineOf(cursor);
		if (this.preferredPx < 0) {
			this.preferredPx = this.px(cursor);
		}
		int target = line + lines;
		if (target < 0) {
			this.buffer.moveTo(0, select);
		} else if (target >= this.lineStarts.length) {
			this.buffer.moveTo(this.buffer.length(), select);
		} else {
			this.buffer.moveTo(this.columnAt(target, this.preferredPx), select);
		}
	}

	private void copy(UiHost host) {
		Text selected = this.buffer.selectedText();
		String clipboard = this.copyFormatter == null ? this.buffer.selectedPlain() : this.copyFormatter.apply(selected);
		host.setClipboard(clipboard);
		RichClipboardUtil.remember(selected, clipboard);
	}

	private void paste(UiHost host) {
		String clipboard = host.clipboard();
		if (clipboard == null || clipboard.isEmpty()) {
			return;
		}
		Text rich = RichClipboardUtil.recall(clipboard);
		if (rich == null && this.pasteParser != null) {
			rich = this.pasteParser.apply(this.multiline ? clipboard : clipboard.replace("\r", "").replace('\n', ' '));
		}
		if (rich == null) {
			this.buffer.insert(clipboard);
		} else {
			this.buffer.insert(rich);
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (!this.focused()) {
			return false;
		}
		boolean shift = input.modifiers().shift();
		boolean word = input.modifiers().primary();
		boolean editable = this.editable && this.interactive();
		UiHost host = this.host();
		String text = this.buffer.text();
		if (this.multiline && (input.key() == Key.UP || input.key() == Key.DOWN)) {
			this.moveVertical(input.key() == Key.UP ? -1 : 1, shift);
			return true;
		}
		if (this.multiline && (input.key() == Key.PAGE_UP || input.key() == Key.PAGE_DOWN)) {
			int lines = this.visibleLines();
			this.moveVertical(input.key() == Key.PAGE_UP ? -lines : lines, shift);
			return true;
		}
		this.preferredPx = -1;
		if (input.isEnter() && !word) {
			if (!this.multiline) {
				return false;
			}
			if (editable) {
				this.buffer.insert("\n");
			}
			return true;
		}
		if (input.shortcut(Key.B) || input.shortcut(Key.I) || input.shortcut(Key.U)) {
			this.toggle(input.key() == Key.B ? Formatting.BOLD : input.key() == Key.I ? Formatting.ITALIC : Formatting.UNDERLINE);
			return true;
		}
		if (input.shortcut(Key.A)) {
			this.buffer.selectAll();
			return true;
		}
		if (input.shortcut(Key.C) || input.shortcut(Key.X)) {
			if (this.buffer.hasSelection() && host != null) {
				this.copy(host);
				if (editable && input.key() == Key.X) {
					this.buffer.deleteSelection();
				}
			}
			return true;
		}
		if (input.shortcut(Key.V)) {
			if (editable && host != null) {
				this.paste(host);
			}
			return true;
		}
		if (input.shortcut(Key.Z)) {
			if (editable) {
				this.buffer.undo();
			}
			return true;
		}
		if (input.shortcut(Key.Y) || input.shiftShortcut(Key.Z)) {
			if (editable) {
				this.buffer.redo();
			}
			return true;
		}
		switch (input.key()) {
			case Key.BACKSPACE:
				if (editable) {
					this.buffer.deleteBackward(word);
				}
				return true;
			case Key.DELETE:
				if (editable) {
					this.buffer.deleteForward(word);
				}
				return true;
			case Key.LEFT:
				if (this.buffer.hasSelection() && !shift && !word) {
					this.buffer.moveTo(this.buffer.selectionStart(), false);
				} else {
					this.buffer.moveTo(word ? TextEditUtil.wordLeft(text, this.buffer.cursor()) : this.buffer.cursor() - 1, shift);
				}
				return true;
			case Key.RIGHT:
				if (this.buffer.hasSelection() && !shift && !word) {
					this.buffer.moveTo(this.buffer.selectionEnd(), false);
				} else {
					this.buffer.moveTo(word ? TextEditUtil.wordRight(text, this.buffer.cursor()) : this.buffer.cursor() + 1, shift);
				}
				return true;
			case Key.HOME:
				this.buffer.moveTo(word || !this.multiline ? 0 : TextEditUtil.lineStart(text, this.buffer.cursor()), shift);
				return true;
			case Key.END:
				this.buffer.moveTo(word || !this.multiline ? text.length() : TextEditUtil.lineEnd(text, this.buffer.cursor()), shift);
				return true;
			default:
				return false;
		}
	}

	@Override
	public boolean acceptsText() {
		return true;
	}

	@Override
	public boolean charTyped(int codepoint, Modifiers modifiers) {
		if (!this.focused() || !this.editable || !this.interactive() || !TextKeyUtil.isTypable(codepoint)) {
			return false;
		}
		this.preferredPx = -1;
		this.buffer.type(new String(Character.toChars(codepoint)));
		return true;
	}
}
