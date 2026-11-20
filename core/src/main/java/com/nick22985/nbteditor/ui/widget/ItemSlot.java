package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class ItemSlot extends Widget {
	private Supplier<GameItem> item;
	private Consumer<MouseInput> onClick;
	private boolean background = true;
	private boolean showTooltip = true;
	private boolean selected;
	private int highlight;

	public ItemSlot(Supplier<GameItem> item) {
		this.item = item;
		this.width = Theme.SLOT;
		this.height = Theme.SLOT;
	}

	public ItemSlot item(Supplier<GameItem> item) {
		this.item = item;
		return this;
	}

	public GameItem item() {
		return this.item == null ? null : this.item.get();
	}

	public ItemSlot onClick(Consumer<MouseInput> onClick) {
		this.onClick = onClick;
		return this;
	}

	public ItemSlot background(boolean background) {
		this.background = background;
		return this;
	}

	public ItemSlot showTooltip(boolean showTooltip) {
		this.showTooltip = showTooltip;
		return this;
	}

	public ItemSlot selected(boolean selected) {
		this.selected = selected;
		return this;
	}

	public ItemSlot highlight(int color) {
		this.highlight = color;
		return this;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		if (this.background) {
			canvas.slot(this.x, this.y);
		}
		if (this.highlight != 0) {
			DrawUtil.slotHighlight(canvas, this.x + 1, this.y + 1, this.highlight);
		}
		GameItem stack = this.item();
		if (stack != null && !stack.isEmpty()) {
			canvas.item(stack, this.x + 1, this.y + 1);
		}
		if (this.selected) {
			DrawUtil.outline(canvas, this.x, this.y, this.width, this.height, Theme.YELLOW);
		}
		if (this.contains(mouseX, mouseY) && this.interactive()) {
			canvas.nextLayer();
			DrawUtil.slotHighlight(canvas, this.x + 1, this.y + 1, Theme.SLOT_HIGHLIGHT);
		}
	}

	@Override
	public List<Text> tooltip(int mouseX, int mouseY) {
		GameItem stack = this.item();
		if (this.showTooltip && stack != null && !stack.isEmpty()) {
			return stack.tooltip(false);
		}
		return super.tooltip(mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (this.onClick != null && this.contains(input.x(), input.y())) {
			this.onClick.accept(input);
			return true;
		}
		return false;
	}
}
