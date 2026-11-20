package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class ItemGrid extends Widget {
	private final int columns;
	private IntFunction<GameItem> items;
	private int count;
	private int selected = -1;
	private BiConsumer<Integer, MouseInput> onClick = (index, input) -> {
	};
	private IntFunction<Integer> highlight = index -> 0;

	public ItemGrid(int columns, int count, IntFunction<GameItem> items) {
		this.columns = columns;
		this.count = count;
		this.items = items;
		this.width = columns * Theme.SLOT;
		this.height = this.rows() * Theme.SLOT;
	}

	public int columns() {
		return this.columns;
	}

	public int rows() {
		return (this.count + this.columns - 1) / this.columns;
	}

	public ItemGrid count(int count) {
		this.count = count;
		this.height = this.rows() * Theme.SLOT;
		return this;
	}

	public ItemGrid items(IntFunction<GameItem> items) {
		this.items = items;
		return this;
	}

	public ItemGrid selected(int selected) {
		this.selected = selected;
		return this;
	}

	public ItemGrid onClick(BiConsumer<Integer, MouseInput> onClick) {
		this.onClick = onClick;
		return this;
	}

	public ItemGrid highlight(IntFunction<Integer> highlight) {
		this.highlight = highlight;
		return this;
	}

	public int indexAt(double mouseX, double mouseY) {
		if (!this.contains(mouseX, mouseY)) {
			return -1;
		}
		int column = (int) (mouseX - this.x) / Theme.SLOT;
		int row = (int) (mouseY - this.y) / Theme.SLOT;
		if (column >= this.columns) {
			return -1;
		}
		int index = row * this.columns + column;
		return index < this.count ? index : -1;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		int hovered = this.interactive() ? this.indexAt(mouseX, mouseY) : -1;
		for (int i = 0; i < this.count; i++) {
			int slotX = this.x + (i % this.columns) * Theme.SLOT;
			int slotY = this.y + (i / this.columns) * Theme.SLOT;
			canvas.slot(slotX, slotY);
			Integer color = this.highlight.apply(i);
			if (color != null && color != 0) {
				DrawUtil.slotHighlight(canvas, slotX + 1, slotY + 1, color);
			}
			GameItem stack = this.items.apply(i);
			if (stack != null && !stack.isEmpty()) {
				canvas.item(stack, slotX + 1, slotY + 1);
			}
			if (i == this.selected) {
				DrawUtil.outline(canvas, slotX, slotY, Theme.SLOT, Theme.SLOT, Theme.YELLOW);
			}
		}
		if (hovered >= 0) {
			canvas.nextLayer();
			DrawUtil.slotHighlight(canvas, this.x + (hovered % this.columns) * Theme.SLOT + 1, this.y + (hovered / this.columns) * Theme.SLOT + 1, Theme.SLOT_HIGHLIGHT);
		}
	}

	@Override
	public List<Text> tooltip(int mouseX, int mouseY) {
		int index = this.indexAt(mouseX, mouseY);
		if (index >= 0) {
			GameItem stack = this.items.apply(index);
			if (stack != null && !stack.isEmpty()) {
				return stack.tooltip(false);
			}
		}
		return super.tooltip(mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		int index = this.indexAt(input.x(), input.y());
		if (index < 0) {
			return false;
		}
		this.onClick.accept(index, input);
		return true;
	}
}
