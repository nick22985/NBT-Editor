package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.text.TextBuffer;
import com.nick22985.nbteditor.ui.text.TextMatch;
import com.nick22985.nbteditor.util.TextSearchUtil;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class FindBar extends Row {
	public static final int GAP = 4;
	private static final int TOGGLE_WIDTH = 20;
	private static final int BUTTON_WIDTH = 56;

	private final TextArea area;
	private final TextField find = new TextField();
	private final TextField replace = new TextField();
	private final Button regexToggle;
	private boolean regex;

	public FindBar(TextArea area, boolean replaceable) {
		super(replaceable ? Theme.FIELD_HEIGHT * 2 + GAP : Theme.FIELD_HEIGHT);
		this.area = area;
		this.place(this.find, 0, 0, -(TOGGLE_WIDTH + BUTTON_WIDTH + GAP * 2), Theme.FIELD_HEIGHT);
		this.find.placeholder(Text.translatable("nbtedit.ui.find.find"));
		this.find.onChange(value -> this.find.error(null));
		this.find.onSubmit(value -> this.next());
		this.regexToggle = this.placeRight(new Button(Text.literal(".*"), this::toggleRegex), BUTTON_WIDTH + GAP, 0, TOGGLE_WIDTH, Theme.BUTTON_HEIGHT);
		this.regexToggle.tooltip(Text.translatable("nbtedit.ui.find.regex"));
		this.placeRight(new Button(Text.translatable("nbtedit.ui.find.next"), this::next), 0, 0, BUTTON_WIDTH, Theme.BUTTON_HEIGHT);
		if (replaceable) {
			int row = Theme.FIELD_HEIGHT + GAP;
			this.place(this.replace, 0, row, -(BUTTON_WIDTH * 2 + GAP * 2), Theme.FIELD_HEIGHT);
			this.replace.placeholder(Text.translatable("nbtedit.ui.find.replace"));
			this.replace.onChange(value -> this.replace.error(null));
			this.replace.onSubmit(value -> this.replaceOne());
			this.placeRight(new Button(Text.translatable("nbtedit.ui.find.replace_one"), this::replaceOne), BUTTON_WIDTH + GAP, row, BUTTON_WIDTH, Theme.BUTTON_HEIGHT);
			this.placeRight(new Button(Text.translatable("nbtedit.ui.find.replace_all"), this::replaceAll), 0, row, BUTTON_WIDTH, Theme.BUTTON_HEIGHT);
		}
		area.onFind(this::focusFind);
	}

	public TextField findField() {
		return this.find;
	}

	public TextField replaceField() {
		return this.replace;
	}

	private void focusFind() {
		TextBuffer buffer = this.area.buffer();
		if (buffer.hasSelection() && buffer.selected().indexOf('\n') < 0) {
			this.find.value(buffer.selected());
		}
		this.find.focus();
		this.find.buffer().selectAll();
	}

	private void toggleRegex() {
		this.regex = !this.regex;
		this.regexToggle.selected(this.regex);
		this.find.error(null);
	}

	private Pattern pattern() {
		String query = this.find.value();
		if (query.isEmpty()) {
			return null;
		}
		try {
			return TextSearchUtil.pattern(query, this.regex);
		} catch (PatternSyntaxException e) {
			this.find.error(Text.literal(e.getDescription()));
			return null;
		}
	}

	private void next() {
		Pattern pattern = this.pattern();
		if (pattern == null) {
			return;
		}
		TextBuffer buffer = this.area.buffer();
		TextMatch match = TextSearchUtil.next(pattern, buffer.text(), buffer.hasSelection() ? buffer.selectionEnd() : buffer.cursor());
		if (match == null) {
			this.find.error(Text.translatable("nbtedit.ui.find.none"));
			return;
		}
		buffer.select(match.start(), match.end());
	}

	private void replaceOne() {
		Pattern pattern = this.pattern();
		if (pattern == null) {
			return;
		}
		TextBuffer buffer = this.area.buffer();
		if (buffer.hasSelection()) {
			try {
				String replacement = TextSearchUtil.expand(pattern, buffer.text(), new TextMatch(buffer.selectionStart(), buffer.selectionEnd()), this.replace.value(), this.regex);
				if (replacement != null) {
					buffer.insert(replacement);
				}
			} catch (IllegalArgumentException | IndexOutOfBoundsException e) {
				this.replace.error(Text.literal(String.valueOf(e.getMessage())));
				return;
			}
		}
		this.next();
	}

	private void replaceAll() {
		Pattern pattern = this.pattern();
		if (pattern == null) {
			return;
		}
		TextBuffer buffer = this.area.buffer();
		String text = buffer.text();
		String result;
		try {
			result = TextSearchUtil.replaceAll(pattern, text, this.replace.value(), this.regex);
		} catch (IllegalArgumentException | IndexOutOfBoundsException e) {
			this.replace.error(Text.literal(String.valueOf(e.getMessage())));
			return;
		}
		if (result.equals(text)) {
			this.find.error(Text.translatable("nbtedit.ui.find.none"));
			return;
		}
		buffer.selectAll();
		buffer.insert(result);
	}
}
