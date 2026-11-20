package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class Checkbox extends Widget {
	public static final int BOX = 10;

	private final Text label;
	private final Consumer<Boolean> onToggle;
	private boolean checked;

	public Checkbox(Text label, boolean checked, Consumer<Boolean> onToggle) {
		this.label = label;
		this.checked = checked;
		this.onToggle = onToggle;
		this.height = Theme.BUTTON_HEIGHT;
	}

	public boolean checked() {
		return this.checked;
	}

	public Checkbox checked(boolean checked) {
		this.checked = checked;
		return this;
	}

	@Override
	public boolean focusable() {
		return true;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		boolean hovered = this.contains(mouseX, mouseY) && this.interactive();
		int boxY = this.y + (this.height - BOX) / 2;
		DrawUtil.rect(canvas, this.x, boxY, BOX, BOX, Theme.FIELD);
		DrawUtil.outline(canvas, this.x, boxY, BOX, BOX, hovered || this.focused() ? Theme.TEXT : this.interactive() ? Theme.FIELD_BORDER : Theme.FIELD_BORDER_READ_ONLY);
		if (this.checked) {
			DrawUtil.sprite(canvas, PixelSprites.CHECK, this.x + 2, boxY + 2, this.interactive() ? Theme.TEXT : Theme.DIM);
		}
		if (this.label != null) {
			int color = !this.interactive() ? Theme.DIM : hovered ? Theme.YELLOW : Theme.TEXT;
			DrawUtil.textClipped(canvas, this.label, this.x + BOX + 4, this.y + (this.height - 8) / 2, this.width - BOX - 4, color);
		}
	}

	private void toggle() {
		this.checked = !this.checked;
		this.onToggle.accept(this.checked);
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (!input.left() || !this.contains(input.x(), input.y())) {
			return false;
		}
		this.playClick();
		this.toggle();
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (this.focused() && (input.plain(Key.SPACE) || input.isEnter())) {
			this.toggle();
			return true;
		}
		return false;
	}
}
