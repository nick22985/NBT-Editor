package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.ColorUtil;
import com.nick22985.nbteditor.util.DrawUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class HueBar extends Widget {
	private final ColorPicker picker;
	private boolean dragging;

	public HueBar(ColorPicker picker) {
		this.picker = picker;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		DrawUtil.outline(canvas, this.x, this.y, this.width, this.height, Theme.SWATCH_BORDER);
		int innerY = this.y + 1;
		int innerHeight = this.height - 2;
		for (int i = 0; i < innerHeight; i++) {
			float hue = innerHeight <= 1 ? 0 : i / (float) (innerHeight - 1);
			canvas.fill(this.x + 1, innerY + i, this.x + this.width - 1, innerY + i + 1, ColorUtil.opaque(ColorUtil.hsvToRgb(hue, 1, 1)));
		}
		int markerY = innerY + Math.round(this.picker.hue() * (innerHeight - 1));
		DrawUtil.rect(canvas, this.x - 1, markerY - 1, this.width + 2, 3, 0xFF000000);
		DrawUtil.rect(canvas, this.x, markerY, this.width, 1, 0xFFFFFFFF);
	}

	private void pick(double mouseY) {
		float hue = (float) ((mouseY - this.y - 1) / Math.max(1, this.height - 3));
		this.picker.hue(Math.max(0, Math.min(0.9999F, hue)));
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (!input.left() || !this.contains(input.x(), input.y())) {
			return false;
		}
		this.dragging = true;
		this.pick(input.y());
		return true;
	}

	@Override
	public boolean mouseDragged(MouseInput input, double deltaX, double deltaY) {
		if (this.dragging) {
			this.pick(input.y());
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseInput input) {
		this.dragging = false;
		return true;
	}
}
