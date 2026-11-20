package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class Slider extends Widget {
	private static final int HANDLE = 8;

	private final double min;
	private final double max;
	private double value;
	private double step;
	private DoubleFunction<Text> label;
	private DoubleConsumer onChange = value -> {
	};
	private boolean dragging;

	public Slider(double min, double max, double value) {
		this.min = min;
		this.max = max;
		this.value = this.clamp(value);
		this.height = Theme.BUTTON_HEIGHT;
	}

	public Slider step(double step) {
		this.step = step;
		return this;
	}

	public Slider label(DoubleFunction<Text> label) {
		this.label = label;
		return this;
	}

	public Slider onChange(DoubleConsumer onChange) {
		this.onChange = onChange;
		return this;
	}

	public double value() {
		return this.value;
	}

	public Slider value(double value) {
		this.value = this.clamp(value);
		return this;
	}

	private double clamp(double value) {
		double clamped = Math.max(this.min, Math.min(this.max, value));
		if (this.step > 0) {
			clamped = this.min + Math.round((clamped - this.min) / this.step) * this.step;
			clamped = Math.max(this.min, Math.min(this.max, clamped));
		}
		return clamped;
	}

	private double progress() {
		return this.max == this.min ? 0 : (this.value - this.min) / (this.max - this.min);
	}

	private void set(double value) {
		double next = this.clamp(value);
		if (next != this.value) {
			this.value = next;
			this.onChange.accept(next);
		}
	}

	@Override
	public boolean focusable() {
		return true;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		boolean hovered = this.contains(mouseX, mouseY) || this.dragging || this.focused();
		canvas.button(this.x, this.y, this.width, this.height, false, false);
		int handleX = this.x + (int) Math.round(this.progress() * (this.width - HANDLE));
		canvas.button(handleX, this.y, HANDLE, this.height, hovered, this.interactive());
		if (this.label != null) {
			Text text = this.label.apply(this.value);
			int color = this.interactive() ? Theme.TEXT : Theme.LABEL;
			int textX = this.x + (this.width - canvas.textWidth(text)) / 2;
			canvas.text(text, textX, this.y + (this.height - 8) / 2, color, true);
		}
	}

	private void dragTo(double mouseX) {
		double progress = (mouseX - this.x - HANDLE / 2.0) / Math.max(1, this.width - HANDLE);
		this.set(this.min + Math.max(0, Math.min(1, progress)) * (this.max - this.min));
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (!input.left() || !this.contains(input.x(), input.y())) {
			return false;
		}
		this.dragging = true;
		this.dragTo(input.x());
		return true;
	}

	@Override
	public boolean mouseDragged(MouseInput input, double deltaX, double deltaY) {
		if (this.dragging) {
			this.dragTo(input.x());
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseInput input) {
		if (this.dragging) {
			this.dragging = false;
			this.playClick();
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (!this.focused()) {
			return false;
		}
		double amount = this.step > 0 ? this.step : (this.max - this.min) / 100.0;
		if (input.plain(Key.LEFT) || input.plain(Key.DOWN)) {
			this.set(this.value - amount);
			return true;
		}
		if (input.plain(Key.RIGHT) || input.plain(Key.UP)) {
			this.set(this.value + amount);
			return true;
		}
		return false;
	}
}
