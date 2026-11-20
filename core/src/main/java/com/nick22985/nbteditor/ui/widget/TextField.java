package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.Modifiers;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.suggest.SuggestionSession;
import com.nick22985.nbteditor.ui.suggest.SuggestionSource;
import com.nick22985.nbteditor.ui.text.TextBuffer;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.TextEditUtil;
import com.nick22985.nbteditor.util.TextKeyUtil;
import com.nick22985.nbteditor.util.TextSliceUtil;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class TextField extends Widget {
	private static final int INSET = 4;

	private final TextBuffer buffer = new TextBuffer(false);
	private Text placeholder;
	private Function<String, Text> formatter;
	private Function<String, Text> validator;
	private Consumer<String> onChange = value -> {
	};
	private Consumer<String> onSubmit;
	private boolean editable = true;
	private boolean framed = true;
	private Text cachedFormatted;
	private String cachedSource;
	private Text error;
	private int scroll;
	private long focusTime;
	private boolean dragging;
	private SuggestionSession suggestions;

	public TextField() {
		this.height = Theme.FIELD_HEIGHT;
		this.buffer.listener(this::changed);
	}

	public TextField(String value) {
		this();
		this.buffer.reset(value);
	}

	private void changed(String value) {
		this.cachedFormatted = null;
		this.error = this.validator == null ? null : this.validator.apply(value);
		this.onChange.accept(value);
		if (this.suggestions != null) {
			this.suggestions.changed();
		}
	}

	public TextBuffer buffer() {
		return this.buffer;
	}

	public String value() {
		return this.buffer.text();
	}

	public TextField value(String value) {
		this.buffer.reset(value);
		if (!this.focused()) {
			this.buffer.moveTo(0, false);
		}
		this.scroll = 0;
		return this;
	}

	public TextField placeholder(Text placeholder) {
		this.placeholder = placeholder;
		return this;
	}

	public TextField formatter(Function<String, Text> formatter) {
		this.formatter = formatter;
		this.cachedFormatted = null;
		return this;
	}

	public TextField validator(Function<String, Text> validator) {
		this.validator = validator;
		this.error = validator == null ? null : validator.apply(this.value());
		return this;
	}

	public TextField maxLength(int maxLength) {
		this.buffer.maxLength(maxLength);
		return this;
	}

	public TextField filter(Predicate<String> filter) {
		this.buffer.filter(filter);
		return this;
	}

	public TextField onChange(Consumer<String> onChange) {
		this.onChange = onChange;
		return this;
	}

	public TextField onSubmit(Consumer<String> onSubmit) {
		this.onSubmit = onSubmit;
		return this;
	}

	public TextField suggestions(SuggestionSource source) {
		if (this.suggestions != null) {
			this.suggestions.hide();
		}
		this.suggestions = source == null ? null : new SuggestionSession(this, this.buffer, source);
		return this;
	}

	public TextField editable(boolean editable) {
		this.editable = editable;
		return this;
	}

	public boolean editable() {
		return this.editable;
	}

	public TextField framed(boolean framed) {
		this.framed = framed;
		return this;
	}

	public Text error() {
		return this.error;
	}

	public boolean valid() {
		return this.error == null;
	}

	public TextField error(Text error) {
		this.error = error;
		return this;
	}

	@Override
	public boolean focusable() {
		return true;
	}

	@Override
	protected void focusChanged(boolean focused) {
		UiHost host = this.host();
		this.focusTime = host == null ? 0 : host.millis();
		if (!focused) {
			this.buffer.moveTo(this.buffer.cursor(), false);
		}
		if (this.suggestions != null) {
			if (!focused) {
				this.suggestions.hide();
			} else if (this.editable && this.buffer.text().isEmpty()) {
				this.suggestions.request();
			}
		}
	}

	private Text formatted() {
		String value = this.buffer.text();
		if (this.cachedFormatted == null || !value.equals(this.cachedSource)) {
			this.cachedSource = value;
			this.cachedFormatted = this.formatter == null ? Text.literal(value) : this.formatter.apply(value);
		}
		return this.cachedFormatted;
	}

	private int prefixWidth(Canvas canvas, int index) {
		if (this.formatter == null) {
			return canvas.textWidth(this.buffer.text().substring(0, index));
		}
		return canvas.textWidth(TextSliceUtil.slice(this.formatted(), 0, index));
	}

	private int innerWidth() {
		return this.width - INSET * 2;
	}

	private int textY() {
		return this.y + (this.height - 8) / 2;
	}

	@Override
	public List<Text> tooltip(int mouseX, int mouseY) {
		if (this.error != null && !this.error.isEmpty()) {
			return List.of(this.error);
		}
		return super.tooltip(mouseX, mouseY);
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		boolean focused = this.focused();
		if (this.framed) {
			int border = this.error != null ? Theme.FIELD_BORDER_ERROR : !this.editable || !this.interactive() ? Theme.FIELD_BORDER_READ_ONLY : focused ? Theme.FIELD_BORDER_FOCUSED : Theme.FIELD_BORDER;
			DrawUtil.field(canvas, this.x, this.y, this.width, this.height, border);
		}
		int innerX = this.x + INSET;
		int innerWidth = this.innerWidth();
		int textY = this.textY();
		String value = this.buffer.text();
		if (value.isEmpty()) {
			if (this.placeholder != null && !focused) {
				DrawUtil.textClipped(canvas, this.placeholder, innerX, textY, innerWidth, Theme.FIELD_PLACEHOLDER);
			}
		}
		int cursorPx = this.prefixWidth(canvas, this.buffer.cursor());
		if (cursorPx - this.scroll > innerWidth - 1) {
			this.scroll = cursorPx - innerWidth + 1;
		}
		if (cursorPx - this.scroll < 0) {
			this.scroll = cursorPx;
		}
		int total = this.prefixWidth(canvas, value.length());
		this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, total - innerWidth + 1)));
		canvas.pushClip(innerX - 1, this.y + 1, innerWidth + 2, this.height - 2);
		int baseX = innerX - this.scroll;
		if (focused && this.buffer.hasSelection()) {
			int startPx = this.prefixWidth(canvas, this.buffer.selectionStart());
			int endPx = this.prefixWidth(canvas, this.buffer.selectionEnd());
			DrawUtil.rect(canvas, baseX + startPx, textY - 1, endPx - startPx, 10, Theme.FIELD_SELECTION);
		}
		if (!value.isEmpty()) {
			int color = this.editable && this.interactive() ? Theme.FIELD_TEXT : Theme.LABEL;
			canvas.text(this.formatted(), baseX, textY, color, true);
		}
		UiHost host = this.host();
		long millis = host == null ? 0 : host.millis();
		if (focused && this.editable && (millis - this.focusTime) / 500 % 2 == 0) {
			int caretX = baseX + cursorPx;
			if (this.buffer.cursor() >= value.length()) {
				canvas.text("_", caretX, textY, Theme.FIELD_TEXT, true);
			} else {
				DrawUtil.rect(canvas, caretX, textY - 1, 1, 10, 0xFFD0D0D0);
			}
		}
		canvas.popClip();
		this.syncSuggestions();
	}

	private void syncSuggestions() {
		UiScreen screen = this.screen();
		if (this.suggestions == null || screen == null) {
			return;
		}
		double[] origin = screen.localize(this, 0, 0);
		this.suggestions.sync((int) Math.round(this.x - origin[0]), (int) Math.round(this.y - origin[1]));
	}

	private int indexAt(double mouseX) {
		UiHost host = this.host();
		if (host == null) {
			return this.buffer.text().length();
		}
		double target = mouseX - (this.x + INSET) + this.scroll;
		String value = this.buffer.text();
		Text formatted = this.formatter == null ? null : this.formatted();
		int previous = 0;
		for (int i = 1; i <= value.length(); i++) {
			int width = formatted == null ? host.textWidth(value.substring(0, i)) : host.textWidth(TextSliceUtil.slice(formatted, 0, i));
			if (target < (previous + width) / 2.0) {
				return i - 1;
			}
			previous = width;
		}
		return value.length();
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (!input.left() || !this.contains(input.x(), input.y())) {
			return false;
		}
		int index = this.indexAt(input.x());
		if (input.doubleClick()) {
			int[] word = TextEditUtil.wordAt(this.buffer.text(), index);
			this.buffer.select(word[0], word[1]);
		} else {
			this.buffer.moveTo(index, input.modifiers().shift());
		}
		this.dragging = true;
		return true;
	}

	@Override
	public boolean mouseDragged(MouseInput input, double deltaX, double deltaY) {
		if (this.dragging) {
			this.buffer.moveTo(this.indexAt(input.x()), true);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseInput input) {
		this.dragging = false;
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (!this.focused()) {
			return false;
		}
		if (this.suggestions != null && this.editable && this.interactive() && this.suggestions.keyPressed(input)) {
			return true;
		}
		if (input.isEnter()) {
			if (this.onSubmit != null) {
				this.onSubmit.accept(this.value());
				return true;
			}
			return false;
		}
		return TextKeyUtil.handleCommon(this.buffer, input, this.host(), this.editable && this.interactive());
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
		this.buffer.type(new String(Character.toChars(codepoint)));
		return true;
	}
}
