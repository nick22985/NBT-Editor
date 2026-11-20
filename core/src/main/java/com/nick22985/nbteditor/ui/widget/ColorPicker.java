package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.WidgetGroup;
import com.nick22985.nbteditor.util.ColorUtil;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.function.IntConsumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class ColorPicker extends WidgetGroup {
	public static final int PREFERRED_HEIGHT = 96;
	private static final int HUE_WIDTH = 10;
	private static final int SWATCH = 10;

	private final ColorArea area = new ColorArea(this);
	private final HueBar hueBar = new HueBar(this);
	private final TextField hex = new TextField();
	private final IntConsumer onChange;
	private final int[] palette;
	private float hue;
	private float saturation;
	private float value;
	private int rgb;
	private boolean updatingHex;

	public ColorPicker(int rgb, int[] palette, IntConsumer onChange) {
		this.onChange = onChange;
		this.palette = palette;
		this.add(this.area);
		this.add(this.hueBar);
		this.add(this.hex);
		this.hex.maxLength(7).placeholder(Text.literal("#RRGGBB")).onChange(this::hexChanged);
		this.hex.validator(text -> ColorUtil.parseHex(text) == null ? Text.translatable("nbtedit.ui.color.invalid") : null);
		this.rgb(rgb);
	}

	public ColorPicker(int rgb, IntConsumer onChange) {
		this(rgb, ColorUtil.FORMATTING_COLORS, onChange);
	}

	public float hue() {
		return this.hue;
	}

	public float saturation() {
		return this.saturation;
	}

	public float value() {
		return this.value;
	}

	public int rgb() {
		return this.rgb;
	}

	public void rgb(int rgb) {
		float[] hsv = ColorUtil.rgbToHsv(rgb);
		if (hsv[1] > 0 && hsv[2] > 0) {
			this.hue = hsv[0];
		}
		this.saturation = hsv[1];
		this.value = hsv[2];
		this.apply(rgb & 0xFFFFFF, true);
	}

	public void hue(float hue) {
		this.hue = hue;
		this.apply(ColorUtil.hsvToRgb(this.hue, this.saturation, this.value), true);
	}

	public void saturationValue(float saturation, float value) {
		this.saturation = saturation;
		this.value = value;
		this.apply(ColorUtil.hsvToRgb(this.hue, this.saturation, this.value), true);
	}

	private void apply(int rgb, boolean updateHex) {
		this.rgb = rgb;
		if (updateHex) {
			this.updatingHex = true;
			this.hex.value(ColorUtil.toHex(rgb));
			this.updatingHex = false;
		}
		this.onChange.accept(rgb);
	}

	private void hexChanged(String text) {
		if (this.updatingHex) {
			return;
		}
		Integer parsed = ColorUtil.parseHex(text);
		if (parsed != null) {
			float[] hsv = ColorUtil.rgbToHsv(parsed);
			if (hsv[1] > 0 && hsv[2] > 0) {
				this.hue = hsv[0];
			}
			this.saturation = hsv[1];
			this.value = hsv[2];
			this.apply(parsed, false);
		}
	}

	private int sideX() {
		return this.x + this.areaSize() + HUE_WIDTH + 8;
	}

	private int areaSize() {
		return Math.min(this.height, Math.max(40, this.width - HUE_WIDTH - 8 - 8 * (SWATCH + 1)));
	}

	private int swatchColumns() {
		return Math.max(1, (this.x + this.width - this.sideX() + 1) / (SWATCH + 1));
	}

	@Override
	protected void onLayout() {
		int size = this.areaSize();
		this.area.bounds(this.x, this.y, size, size);
		this.hueBar.bounds(this.x + size + 4, this.y, HUE_WIDTH, size);
		int sideX = this.sideX();
		this.hex.bounds(sideX, this.y, Math.max(50, this.x + this.width - sideX - 20), Theme.FIELD_HEIGHT);
	}

	private int swatchTop() {
		return this.y + Theme.FIELD_HEIGHT + 6;
	}

	private int swatchAt(double mouseX, double mouseY) {
		int columns = this.swatchColumns();
		for (int i = 0; i < this.palette.length; i++) {
			int swatchX = this.sideX() + (i % columns) * (SWATCH + 1);
			int swatchY = this.swatchTop() + (i / columns) * (SWATCH + 1);
			if (DrawUtil.inside(mouseX, mouseY, swatchX, swatchY, SWATCH, SWATCH)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	protected void renderForeground(Canvas canvas, int mouseX, int mouseY, float delta) {
		int previewX = this.hex.right() + 4;
		DrawUtil.swatch(canvas, previewX, this.y, Math.max(8, this.x + this.width - previewX), Theme.FIELD_HEIGHT, this.rgb, false);
		int hovered = this.swatchAt(mouseX, mouseY);
		int columns = this.swatchColumns();
		for (int i = 0; i < this.palette.length; i++) {
			int swatchX = this.sideX() + (i % columns) * (SWATCH + 1);
			int swatchY = this.swatchTop() + (i / columns) * (SWATCH + 1);
			DrawUtil.swatch(canvas, swatchX, swatchY, SWATCH, SWATCH, this.palette[i], hovered == i || this.palette[i] == this.rgb);
		}
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		int index = this.swatchAt(input.x(), input.y());
		if (index >= 0 && input.left()) {
			this.playClick();
			this.rgb(this.palette[index]);
			return true;
		}
		return false;
	}
}
