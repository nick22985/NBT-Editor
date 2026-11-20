package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.NumberUtil;
import java.util.function.DoubleConsumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class NumberField extends TextField {
	private static final int ARROWS = 9;

	private final boolean integer;
	private double min = -Double.MAX_VALUE;
	private double max = Double.MAX_VALUE;
	private double step = 1;
	private DoubleConsumer onValue = value -> {
	};
	private Runnable onEmpty;

	public NumberField(boolean integer, double value) {
		super(integer ? Long.toString((long) value) : NumberUtil.format(value));
		this.integer = integer;
		this.filter(text -> text.isEmpty() || text.matches(integer ? "-?\\d*" : "-?\\d*\\.?\\d*([eE]-?\\d*)?"));
		this.validator(this::validate);
		this.onChange(text -> {
			if (text.isEmpty() && this.onEmpty != null) {
				this.onEmpty.run();
				return;
			}
			Double parsed = this.parse(text);
			if (parsed != null && parsed >= this.min && parsed <= this.max) {
				this.onValue.accept(parsed);
			}
		});
	}

	public NumberField range(double min, double max) {
		this.min = min;
		this.max = max;
		this.validator(this::validate);
		return this;
	}

	public NumberField step(double step) {
		this.step = step;
		return this;
	}

	public NumberField onValue(DoubleConsumer onValue) {
		this.onValue = onValue;
		return this;
	}

	public NumberField optional(Runnable onEmpty) {
		this.onEmpty = onEmpty;
		this.validator(this::validate);
		return this;
	}

	private Double parse(String text) {
		try {
			return this.integer ? (double) Long.parseLong(text) : Double.parseDouble(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Text validate(String text) {
		if (text.isEmpty() && this.onEmpty != null) {
			return null;
		}
		Double parsed = this.parse(text);
		if (parsed == null) {
			return Text.translatable("nbtedit.ui.number.invalid");
		}
		if (parsed < this.min || parsed > this.max) {
			return Text.translatable("nbtedit.ui.number.range", NumberUtil.format(this.min), NumberUtil.format(this.max));
		}
		return null;
	}

	public Double number() {
		Double parsed = this.parse(this.value());
		return parsed != null && parsed >= this.min && parsed <= this.max ? parsed : null;
	}

	public void number(double value) {
		this.value(this.integer ? Long.toString((long) value) : NumberUtil.format(value));
	}

	private void nudge(double amount) {
		Double current = this.parse(this.value());
		double next = Math.max(this.min, Math.min(this.max, (current == null ? 0 : current) + amount));
		this.buffer().set(this.integer ? Long.toString((long) next) : NumberUtil.format(next));
		this.buffer().moveTo(this.value().length(), false);
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		super.render(canvas, mouseX, mouseY, delta);
		if (!this.editable() || this.width < 40) {
			return;
		}
		int arrowX = this.x + this.width - ARROWS - 1;
		int mid = this.y + this.height / 2;
		boolean hoverUp = this.interactive() && DrawUtil.inside(mouseX, mouseY, arrowX, this.y, ARROWS, this.height / 2);
		boolean hoverDown = this.interactive() && DrawUtil.inside(mouseX, mouseY, arrowX, mid, ARROWS, this.height - this.height / 2);
		DrawUtil.rect(canvas, arrowX, this.y + 1, ARROWS, this.height - 2, Theme.FIELD);
		DrawUtil.spriteCentered(canvas, PixelSprites.UP, arrowX, this.y + 1, ARROWS, this.height / 2 - 1, hoverUp ? Theme.YELLOW : Theme.LABEL);
		DrawUtil.spriteCentered(canvas, PixelSprites.DOWN, arrowX, mid, ARROWS, this.height / 2 - 1, hoverDown ? Theme.YELLOW : Theme.LABEL);
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (input.left() && this.editable() && this.width >= 40 && this.contains(input.x(), input.y()) && input.x() >= this.x + this.width - ARROWS - 1) {
			this.playClick();
			double amount = input.modifiers().shift() ? this.step * 10 : this.step;
			this.nudge(input.y() < this.y + this.height / 2.0 ? amount : -amount);
			return true;
		}
		return super.mouseClicked(input);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		if (!this.focused() || !this.editable() || deltaY == 0) {
			return false;
		}
		this.nudge(Math.signum(deltaY) * this.step);
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (this.focused() && this.editable() && (input.is(Key.UP) || input.is(Key.DOWN)) && !input.modifiers().primary()) {
			double amount = input.modifiers().shift() ? this.step * 10 : this.step;
			this.nudge(input.key() == Key.UP ? amount : -amount);
			return true;
		}
		return super.keyPressed(input);
	}
}
