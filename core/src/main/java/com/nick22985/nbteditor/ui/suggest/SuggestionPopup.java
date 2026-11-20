package com.nick22985.nbteditor.ui.suggest;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class SuggestionPopup extends Widget {
	private static final int ROW_HEIGHT = 12;
	private static final int PADDING = 2;
	private static final int MAX_ROWS = 8;
	private static final int MIN_WIDTH = 60;
	private static final int MAX_WIDTH = 240;

	private final Consumer<TextSuggestion> onPick;
	private List<TextSuggestion> entries = List.of();
	private int selected;
	private int scroll;
	private boolean navigated;

	public SuggestionPopup(Consumer<TextSuggestion> onPick) {
		this.onPick = onPick;
	}

	public List<TextSuggestion> entries() {
		return this.entries;
	}

	public void entries(List<TextSuggestion> entries) {
		this.entries = List.copyOf(entries);
		this.selected = 0;
		this.scroll = 0;
		this.navigated = false;
	}

	public boolean navigated() {
		return this.navigated;
	}

	public TextSuggestion selected() {
		return this.entries.isEmpty() ? null : this.entries.get(this.selected);
	}

	public void move(int step) {
		if (this.entries.isEmpty()) {
			return;
		}
		this.navigated = true;
		this.selected = Math.floorMod(this.selected + step, this.entries.size());
		if (this.selected < this.scroll) {
			this.scroll = this.selected;
		} else if (this.selected >= this.scroll + MAX_ROWS) {
			this.scroll = this.selected - MAX_ROWS + 1;
		}
	}

	private int rows() {
		return Math.min(MAX_ROWS, this.entries.size());
	}

	public void place(int anchorX, int anchorY, int anchorHeight, int screenWidth, int screenHeight) {
		UiHost host = this.host();
		int widest = 0;
		for (TextSuggestion entry : this.entries) {
			widest = Math.max(widest, host == null ? 0 : host.textWidth(entry.text()));
		}
		int width = DrawUtil.clamp(widest + 8 + PADDING * 2, MIN_WIDTH, MAX_WIDTH);
		int height = this.rows() * ROW_HEIGHT + PADDING * 2;
		int below = anchorY + anchorHeight + 1;
		int y = below + height > screenHeight - 2 && anchorY - height - 1 >= 2 ? anchorY - height - 1 : below;
		int x = DrawUtil.clamp(anchorX, 2, Math.max(2, screenWidth - width - 2));
		this.bounds(x, y, width, height);
	}

	private int indexAt(double mouseX, double mouseY) {
		if (!this.contains(mouseX, mouseY)) {
			return -1;
		}
		int row = (int) Math.floor((mouseY - this.y - PADDING) / ROW_HEIGHT);
		int index = this.scroll + row;
		return row < 0 || row >= this.rows() || index >= this.entries.size() ? -1 : index;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		if (this.entries.isEmpty()) {
			return;
		}
		DrawUtil.rect(canvas, this.x, this.y, this.width, this.height, Theme.POPOVER);
		DrawUtil.outline(canvas, this.x, this.y, this.width, this.height, Theme.FAINT);
		int hovered = this.indexAt(mouseX, mouseY);
		for (int row = 0; row < this.rows(); row++) {
			int index = this.scroll + row;
			int rowY = this.y + PADDING + row * ROW_HEIGHT;
			boolean active = index == this.selected;
			if (active || index == hovered) {
				DrawUtil.rect(canvas, this.x + PADDING, rowY, this.width - PADDING * 2, ROW_HEIGHT, Theme.POPOVER_HOVER);
			}
			DrawUtil.textClipped(canvas, this.entries.get(index).text(), this.x + PADDING + 4, rowY + 2, this.width - PADDING * 2 - 6, active ? Theme.YELLOW : Theme.FIELD_TEXT);
		}
		if (this.entries.size() > MAX_ROWS) {
			int trackHeight = this.height - PADDING * 2;
			int thumbHeight = Math.max(4, trackHeight * MAX_ROWS / this.entries.size());
			int thumbY = this.y + PADDING + (trackHeight - thumbHeight) * this.scroll / (this.entries.size() - MAX_ROWS);
			DrawUtil.rect(canvas, this.right() - PADDING - 1, thumbY, 1, thumbHeight, Theme.FAINT);
		}
	}

	@Override
	public List<Text> tooltip(int mouseX, int mouseY) {
		int index = this.indexAt(mouseX, mouseY);
		Text tooltip = index < 0 ? null : this.entries.get(index).tooltip();
		return tooltip == null || tooltip.isEmpty() ? null : List.of(tooltip);
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		int index = this.indexAt(input.x(), input.y());
		if (index >= 0 && input.left()) {
			this.playClick();
			this.onPick.accept(this.entries.get(index));
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		int max = Math.max(0, this.entries.size() - MAX_ROWS);
		this.scroll = DrawUtil.clamp(this.scroll - (int) Math.signum(deltaY), 0, max);
		return true;
	}
}
