package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Overlay;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class PopoverMenu extends Overlay {
	private static final int CELL_HEIGHT = 12;
	private static final int SEPARATOR_HEIGHT = 5;
	private static final int PADDING = 2;

	private final List<MenuOption> options;
	private final int anchorX;
	private final int anchorY;
	private final boolean upwards;
	private int columns = 1;
	private int cellWidth;
	private int minWidth;
	private int keyboardIndex = -1;
	private int lastMouseX;
	private int lastMouseY;

	public PopoverMenu(int anchorX, int anchorY, boolean upwards, List<MenuOption> options) {
		this.options = List.copyOf(options);
		this.anchorX = anchorX;
		this.anchorY = anchorY;
		this.upwards = upwards;
	}

	public PopoverMenu columns(int columns) {
		this.columns = Math.max(1, columns);
		return this;
	}

	public PopoverMenu minWidth(int minWidth) {
		this.minWidth = minWidth;
		return this;
	}

	@Override
	public boolean dimBackground() {
		return false;
	}

	@Override
	public boolean closeOnOutsideClick() {
		return true;
	}

	private boolean hasChecks() {
		for (MenuOption option : this.options) {
			if (option.checked()) {
				return true;
			}
		}
		return false;
	}

	private int columnCount() {
		return Math.max(1, Math.min(this.columns, this.options.size()));
	}

	@Override
	public void layout(int screenWidth, int screenHeight) {
		UiHost host = this.host();
		int widest = 0;
		int checkWidth = this.hasChecks() ? DrawUtil.spriteWidth(PixelSprites.CHECK) + 4 : 0;
		for (MenuOption option : this.options) {
			if (option.isSeparator()) {
				continue;
			}
			int optionWidth = host == null ? 0 : host.textWidth(option.label());
			if (option.chip() != null && host != null) {
				optionWidth += DrawUtil.chipWidth(host, option.chip()) + 4;
			}
			widest = Math.max(widest, optionWidth + checkWidth);
		}
		int columns = this.columnCount();
		this.cellWidth = Math.max(widest + 8, (this.minWidth - PADDING * 2) / columns);
		int width = this.cellWidth * columns + PADDING * 2;
		int height = this.contentHeight() + PADDING * 2;
		int x = DrawUtil.clamp(this.anchorX, 2, Math.max(2, screenWidth - width - 2));
		int y = DrawUtil.clamp(this.upwards ? this.anchorY - height : this.anchorY, 2, Math.max(2, screenHeight - height - 2));
		this.bounds(x, y, width, height);
	}

	private int contentHeight() {
		if (this.columnCount() > 1) {
			int rows = (this.options.size() + this.columnCount() - 1) / this.columnCount();
			return rows * CELL_HEIGHT;
		}
		int height = 0;
		for (MenuOption option : this.options) {
			height += option.isSeparator() ? SEPARATOR_HEIGHT : CELL_HEIGHT;
		}
		return height;
	}

	private int cellX(int index) {
		return this.x + PADDING + (index % this.columnCount()) * this.cellWidth;
	}

	private int cellY(int index) {
		if (this.columnCount() > 1) {
			return this.y + PADDING + (index / this.columnCount()) * CELL_HEIGHT;
		}
		int top = this.y + PADDING;
		for (int i = 0; i < index; i++) {
			top += this.options.get(i).isSeparator() ? SEPARATOR_HEIGHT : CELL_HEIGHT;
		}
		return top;
	}

	private int indexAt(double mouseX, double mouseY) {
		for (int i = 0; i < this.options.size(); i++) {
			MenuOption option = this.options.get(i);
			if (option.isSeparator()) {
				continue;
			}
			if (DrawUtil.inside(mouseX, mouseY, this.cellX(i), this.cellY(i), this.cellWidth, CELL_HEIGHT)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		if (mouseX != this.lastMouseX || mouseY != this.lastMouseY) {
			this.lastMouseX = mouseX;
			this.lastMouseY = mouseY;
			int hoveredIndex = this.indexAt(mouseX, mouseY);
			if (hoveredIndex >= 0) {
				this.keyboardIndex = hoveredIndex;
			}
		}
		DrawUtil.rect(canvas, this.x, this.y, this.width, this.height, Theme.POPOVER);
		DrawUtil.outline(canvas, this.x, this.y, this.width, this.height, Theme.TEXT);
		int hovered = this.indexAt(mouseX, mouseY);
		if (hovered < 0) {
			hovered = this.keyboardIndex;
		}
		boolean checks = this.hasChecks();
		for (int i = 0; i < this.options.size(); i++) {
			MenuOption option = this.options.get(i);
			int cellX = this.cellX(i);
			int cellY = this.cellY(i);
			if (option.isSeparator()) {
				DrawUtil.rect(canvas, cellX + 2, cellY + 2, this.cellWidth - 4, 1, Theme.SECTION_LINE);
				continue;
			}
			boolean active = option.enabled() && option.action() != null;
			if (i == hovered && active) {
				DrawUtil.rect(canvas, cellX, cellY, this.cellWidth, CELL_HEIGHT, Theme.POPOVER_HOVER);
			}
			int textX = cellX + 4;
			if (checks) {
				if (option.checked()) {
					DrawUtil.sprite(canvas, PixelSprites.CHECK, textX, cellY + 3, Theme.GREEN);
				}
				textX += DrawUtil.spriteWidth(PixelSprites.CHECK) + 4;
			}
			if (option.chip() != null) {
				textX += DrawUtil.chip(canvas, option.chip(), textX, cellY + 1, option.chipColor()) + 4;
			}
			int color = !active ? Theme.DIM : i == hovered ? Theme.YELLOW : Theme.TEXT;
			DrawUtil.textClipped(canvas, option.label(), textX, cellY + 2, cellX + this.cellWidth - textX - 2, color);
		}
	}

	@Override
	public List<Text> tooltip(int mouseX, int mouseY) {
		int index = this.indexAt(mouseX, mouseY);
		if (index >= 0 && this.options.get(index).tooltip() != null) {
			return List.of(this.options.get(index).tooltip());
		}
		return null;
	}

	private void run(int index) {
		if (index < 0 || index >= this.options.size()) {
			return;
		}
		MenuOption option = this.options.get(index);
		if (option.isSeparator() || !option.enabled() || option.action() == null) {
			return;
		}
		this.playClick();
		this.close();
		option.action().run();
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (!this.contains(input.x(), input.y())) {
			this.close();
			return true;
		}
		if (input.left()) {
			this.run(this.indexAt(input.x(), input.y()));
		}
		return true;
	}

	private void moveKeyboard(int step) {
		int size = this.options.size();
		if (size == 0) {
			return;
		}
		int index = this.keyboardIndex;
		for (int i = 0; i < size; i++) {
			index = Math.floorMod(index + step, size);
			MenuOption option = this.options.get(index);
			if (!option.isSeparator() && option.enabled() && option.action() != null) {
				this.keyboardIndex = index;
				return;
			}
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		int columns = this.columnCount();
		if (input.plain(Key.DOWN)) {
			this.moveKeyboard(columns);
			return true;
		}
		if (input.plain(Key.UP)) {
			this.moveKeyboard(-columns);
			return true;
		}
		if (columns > 1 && input.plain(Key.RIGHT)) {
			this.moveKeyboard(1);
			return true;
		}
		if (columns > 1 && input.plain(Key.LEFT)) {
			this.moveKeyboard(-1);
			return true;
		}
		if (input.isEnter() || input.plain(Key.SPACE)) {
			this.run(this.keyboardIndex);
			return true;
		}
		return false;
	}
}
