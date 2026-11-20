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
public class ColorArea extends Widget {
	private final ColorPicker picker;
	private boolean dragging;

	public ColorArea(ColorPicker picker) {
		this.picker = picker;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		DrawUtil.outline(canvas, this.x, this.y, this.width, this.height, Theme.SWATCH_BORDER);
		int innerX = this.x + 1;
		int innerY = this.y + 1;
		int innerWidth = this.width - 2;
		int innerHeight = this.height - 2;
		for (int i = 0; i < innerWidth; i++) {
			float saturation = innerWidth <= 1 ? 0 : i / (float) (innerWidth - 1);
			int top = ColorUtil.opaque(ColorUtil.hsvToRgb(this.picker.hue(), saturation, 1));
			canvas.gradient(innerX + i, innerY, innerX + i + 1, innerY + innerHeight, top, 0xFF000000);
		}
		int markerX = innerX + Math.round(this.picker.saturation() * (innerWidth - 1));
		int markerY = innerY + Math.round((1 - this.picker.value()) * (innerHeight - 1));
		int markerColor = this.picker.value() > 0.5F && this.picker.saturation() < 0.5F ? 0xFF000000 : 0xFFFFFFFF;
		DrawUtil.outline(canvas, markerX - 2, markerY - 2, 5, 5, markerColor);
	}

	private void pick(double mouseX, double mouseY) {
		float saturation = (float) ((mouseX - this.x - 1) / Math.max(1, this.width - 3));
		float value = 1 - (float) ((mouseY - this.y - 1) / Math.max(1, this.height - 3));
		this.picker.saturationValue(Math.max(0, Math.min(1, saturation)), Math.max(0, Math.min(1, value)));
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (!input.left() || !this.contains(input.x(), input.y())) {
			return false;
		}
		this.dragging = true;
		this.pick(input.x(), input.y());
		return true;
	}

	@Override
	public boolean mouseDragged(MouseInput input, double deltaX, double deltaY) {
		if (this.dragging) {
			this.pick(input.x(), input.y());
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
