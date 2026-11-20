package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.Modifiers;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.TextMetrics;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.text.TextBuffer;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.TextEditUtil;
import com.nick22985.nbteditor.util.TextKeyUtil;
import com.nick22985.nbteditor.util.TextSliceUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class TextArea extends Widget {
	private static final int INSET = 4;

	private final TextBuffer buffer = new TextBuffer(true);
	private Function<String, Text> formatter;
	private Consumer<String> onChange = value -> {
	};
	private Runnable onFind;
	private Text placeholder;
	private boolean editable = true;
	private boolean lineNumbers;
	private String indent = "  ";
	private String cachedSource;
	private List<String> rawLines = List.of("");
	private List<Text> formattedLines = List.of(Text.EMPTY);
	private int[] lineStarts = {0};
	private int scrollY;
	private int scrollX;
	private int preferredColumnPx = -1;
	private boolean dragging;
	private long focusTime;
	private int errorIndex = -1;
	private Text error;
	private int lastCursor = -1;

	public TextArea() {
		this.buffer.listener(this::changed);
	}

	private void changed(String value) {
		this.onChange.accept(value);
	}

	public TextBuffer buffer() {
		return this.buffer;
	}

	public String value() {
		return this.buffer.text();
	}

	public TextArea value(String value) {
		this.buffer.reset(value);
		this.scrollX = 0;
		this.scrollY = 0;
		return this;
	}

	public TextArea formatter(Function<String, Text> formatter) {
		this.formatter = formatter;
		this.cachedSource = null;
		return this;
	}

	public TextArea onChange(Consumer<String> onChange) {
		this.onChange = onChange;
		return this;
	}

	public TextArea onFind(Runnable onFind) {
		this.onFind = onFind;
		return this;
	}

	public TextArea placeholder(Text placeholder) {
		this.placeholder = placeholder;
		return this;
	}

	public TextArea editable(boolean editable) {
		this.editable = editable;
		return this;
	}

	public TextArea lineNumbers(boolean lineNumbers) {
		this.lineNumbers = lineNumbers;
		return this;
	}

	public TextArea indent(String indent) {
		this.indent = indent;
		return this;
	}

	public TextArea maxLength(int maxLength) {
		this.buffer.maxLength(maxLength);
		return this;
	}

	public TextArea error(int index, Text message) {
		this.errorIndex = index;
		this.error = message;
		return this;
	}

	public TextArea clearError() {
		this.errorIndex = -1;
		this.error = null;
		return this;
	}

	public Text error() {
		return this.error;
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

	private void refreshLines() {
		String value = this.buffer.text();
		if (value.equals(this.cachedSource)) {
			return;
		}
		this.cachedSource = value;
		this.rawLines = List.of(value.split("\n", -1));
		if (this.formatter == null) {
			List<Text> lines = new ArrayList<>();
			for (String line : this.rawLines) {
				lines.add(Text.literal(line));
			}
			this.formattedLines = lines;
		} else {
			List<Text> lines = TextSliceUtil.lines(this.formatter.apply(value));
			while (lines.size() < this.rawLines.size()) {
				lines.add(Text.EMPTY);
			}
			this.formattedLines = lines;
		}
		this.lineStarts = new int[this.rawLines.size()];
		int start = 0;
		for (int i = 0; i < this.rawLines.size(); i++) {
			this.lineStarts[i] = start;
			start += this.rawLines.get(i).length() + 1;
		}
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

	private int gutterWidth(TextMetrics metrics) {
		if (!this.lineNumbers) {
			return 0;
		}
		return metrics.textWidth(Integer.toString(this.rawLines.size())) + 6;
	}

	private int textLeft(TextMetrics metrics) {
		return this.x + INSET + this.gutterWidth(metrics);
	}

	private int textWidthAvailable(TextMetrics metrics) {
		return this.width - INSET * 2 - this.gutterWidth(metrics) - Theme.SCROLLBAR_WIDTH - 1;
	}

	private int visibleLines() {
		return Math.max(1, (this.height - INSET * 2) / Theme.LINE);
	}

	private int columnPx(TextMetrics metrics, int line, int column) {
		if (this.formatter == null) {
			return metrics.textWidth(this.rawLines.get(line).substring(0, column));
		}
		return metrics.textWidth(TextSliceUtil.slice(this.formattedLines.get(line), 0, column));
	}

	private int columnAt(TextMetrics metrics, int line, double px) {
		String raw = this.rawLines.get(line);
		int previous = 0;
		for (int i = 1; i <= raw.length(); i++) {
			int width = this.columnPx(metrics, line, i);
			if (px < (previous + width) / 2.0) {
				return i - 1;
			}
			previous = width;
		}
		return raw.length();
	}

	private int contentHeight() {
		return this.rawLines.size() * Theme.LINE + INSET * 2;
	}

	private void clampScroll() {
		int maxScroll = Math.max(0, this.contentHeight() - this.height);
		this.scrollY = DrawUtil.clamp(this.scrollY, 0, maxScroll);
		this.scrollX = Math.max(0, this.scrollX);
	}

	private void followCursor(TextMetrics metrics) {
		int cursor = this.buffer.cursor();
		if (cursor == this.lastCursor) {
			return;
		}
		this.lastCursor = cursor;
		int line = this.lineOf(cursor);
		int top = line * Theme.LINE;
		int view = this.height - INSET * 2;
		if (top < this.scrollY) {
			this.scrollY = top;
		} else if (top + Theme.LINE > this.scrollY + view) {
			this.scrollY = top + Theme.LINE - view;
		}
		int px = this.columnPx(metrics, line, cursor - this.lineStarts[line]);
		int available = this.textWidthAvailable(metrics);
		if (px - this.scrollX > available - 6) {
			this.scrollX = px - available + 6;
		} else if (px - this.scrollX < 0) {
			this.scrollX = Math.max(0, px - 20);
		}
	}

	@Override
	public List<Text> tooltip(int mouseX, int mouseY) {
		if (this.error != null) {
			return List.of(this.error);
		}
		return super.tooltip(mouseX, mouseY);
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		this.refreshLines();
		boolean focused = this.focused();
		if (focused) {
			this.followCursor(canvas);
		}
		this.clampScroll();
		int border = this.error != null ? Theme.FIELD_BORDER_ERROR : !this.editable ? Theme.FIELD_BORDER_READ_ONLY : focused ? Theme.FIELD_BORDER_FOCUSED : Theme.FIELD_BORDER;
		DrawUtil.field(canvas, this.x, this.y, this.width, this.height, border);
		int gutter = this.gutterWidth(canvas);
		int left = this.textLeft(canvas);
		int top = this.y + INSET;
		int first = this.scrollY / Theme.LINE;
		int last = Math.min(this.rawLines.size() - 1, (this.scrollY + this.height) / Theme.LINE);
		canvas.pushClip(this.x + 1, this.y + 1, this.width - 2, this.height - 2);
		if (this.buffer.text().isEmpty() && this.placeholder != null && !focused) {
			DrawUtil.textClipped(canvas, this.placeholder, left, top, this.textWidthAvailable(canvas), Theme.FIELD_PLACEHOLDER);
		}
		if (gutter > 0) {
			DrawUtil.rect(canvas, this.x + 1, this.y + 1, gutter + INSET - 2, this.height - 2, 0x40000000);
			for (int line = first; line <= last; line++) {
				String number = Integer.toString(line + 1);
				int lineY = top + line * Theme.LINE - this.scrollY;
				canvas.text(number, this.x + INSET + gutter - 4 - canvas.textWidth(number), lineY, Theme.DIM, false);
			}
		}
		canvas.pushClip(left - 1, this.y + 1, this.textWidthAvailable(canvas) + 2, this.height - 2);
		int selectionStart = this.buffer.selectionStart();
		int selectionEnd = this.buffer.selectionEnd();
		boolean selection = focused && this.buffer.hasSelection();
		int textColor = this.editable ? Theme.FIELD_TEXT : Theme.LABEL;
		for (int line = first; line <= last; line++) {
			int lineY = top + line * Theme.LINE - this.scrollY;
			int lineStart = this.lineStarts[line];
			int lineEnd = lineStart + this.rawLines.get(line).length();
			if (selection && selectionStart <= lineEnd && selectionEnd >= lineStart) {
				int from = Math.max(selectionStart, lineStart) - lineStart;
				int to = Math.min(selectionEnd, lineEnd) - lineStart;
				int fromPx = this.columnPx(canvas, line, from);
				int toPx = this.columnPx(canvas, line, to) + (selectionEnd > lineEnd ? 4 : 0);
				DrawUtil.rect(canvas, left + fromPx - this.scrollX, lineY - 1, Math.max(1, toPx - fromPx), Theme.LINE, Theme.FIELD_SELECTION);
			}
			canvas.text(this.formattedLines.get(line), left - this.scrollX, lineY, textColor, true);
			if (this.errorIndex >= lineStart && this.errorIndex <= lineEnd) {
				int column = this.errorIndex - lineStart;
				int errorPx = this.columnPx(canvas, line, column);
				int errorWidth = column < this.rawLines.get(line).length() ? Math.max(3, this.columnPx(canvas, line, column + 1) - errorPx) : 4;
				DrawUtil.rect(canvas, left + errorPx - this.scrollX, lineY + 8, errorWidth, 1, Theme.RED);
			}
		}
		UiHost host = this.host();
		long millis = host == null ? 0 : host.millis();
		if (focused && this.editable && (millis - this.focusTime) / 500 % 2 == 0) {
			int cursor = this.buffer.cursor();
			int line = this.lineOf(cursor);
			int caretX = left + this.columnPx(canvas, line, cursor - this.lineStarts[line]) - this.scrollX;
			DrawUtil.rect(canvas, caretX, top + line * Theme.LINE - this.scrollY - 1, 1, Theme.LINE, 0xFFD0D0D0);
		}
		canvas.popClip();
		canvas.popClip();
		DrawUtil.scrollbar(canvas, this.x + this.width - 1 - Theme.SCROLLBAR_WIDTH, this.y + 1, this.height - 2, this.contentHeight(), this.scrollY);
	}

	private int indexAt(double mouseX, double mouseY) {
		UiHost host = this.host();
		this.refreshLines();
		int line = DrawUtil.clamp((int) Math.floor((mouseY - this.y - INSET + this.scrollY) / Theme.LINE), 0, this.rawLines.size() - 1);
		if (host == null) {
			return this.lineStarts[line];
		}
		int column = this.columnAt(host, line, mouseX - this.textLeft(host) + this.scrollX);
		return this.lineStarts[line] + column;
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
		this.preferredColumnPx = -1;
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
		this.refreshLines();
		int before = this.scrollY;
		int beforeX = this.scrollX;
		UiHost host = this.host();
		if (host != null && host.modifiers().shift()) {
			this.scrollX -= (int) Math.round(deltaY * 20);
		} else {
			this.scrollY -= (int) Math.round(deltaY * Theme.LINE * 3);
			this.scrollX -= (int) Math.round(deltaX * 20);
		}
		this.clampScroll();
		return before != this.scrollY || beforeX != this.scrollX;
	}

	private void moveVertical(int lines, boolean select) {
		UiHost host = this.host();
		this.refreshLines();
		int cursor = this.buffer.cursor();
		int line = this.lineOf(cursor);
		if (host == null) {
			return;
		}
		if (this.preferredColumnPx < 0) {
			this.preferredColumnPx = this.columnPx(host, line, cursor - this.lineStarts[line]);
		}
		int target = line + lines;
		if (target < 0) {
			this.buffer.moveTo(0, select);
			return;
		}
		if (target >= this.rawLines.size()) {
			this.buffer.moveTo(this.buffer.text().length(), select);
			return;
		}
		int column = this.columnAt(host, target, this.preferredColumnPx);
		this.buffer.moveTo(this.lineStarts[target] + column, select);
	}

	private String currentIndent() {
		String text = this.buffer.text();
		int start = TextEditUtil.lineStart(text, this.buffer.cursor());
		int end = start;
		while (end < text.length() && (text.charAt(end) == ' ' || text.charAt(end) == '\t')) {
			end++;
		}
		return text.substring(start, Math.min(end, this.buffer.cursor()));
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (!this.focused()) {
			return false;
		}
		if (this.onFind != null && input.shortcut(Key.F)) {
			this.onFind.run();
			return true;
		}
		boolean shift = input.modifiers().shift();
		boolean editable = this.editable && this.interactive();
		if (input.key() == Key.UP || input.key() == Key.DOWN) {
			this.moveVertical(input.key() == Key.UP ? -1 : 1, shift);
			return true;
		}
		if (input.key() == Key.PAGE_UP || input.key() == Key.PAGE_DOWN) {
			int lines = this.visibleLines();
			this.moveVertical(input.key() == Key.PAGE_UP ? -lines : lines, shift);
			return true;
		}
		this.preferredColumnPx = -1;
		if (input.isEnter() && !input.modifiers().primary()) {
			if (editable) {
				this.buffer.insert("\n" + this.currentIndent());
			}
			return true;
		}
		if (input.key() == Key.TAB && !input.modifiers().primary()) {
			if (editable && !shift) {
				this.buffer.insert(this.indent);
				return true;
			}
			return false;
		}
		return TextKeyUtil.handleCommon(this.buffer, input, this.host(), editable);
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
		this.preferredColumnPx = -1;
		this.buffer.type(new String(Character.toChars(codepoint)));
		return true;
	}
}
