package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class Dropdown<T> extends Button {
	private final List<T> values;
	private final Function<T, Text> labeler;
	private final Consumer<T> onChange;
	private T value;
	private int columns = 1;

	public Dropdown(List<T> values, T value, Function<T, Text> labeler, Consumer<T> onChange) {
		super(Text.EMPTY, button -> {
		});
		this.values = List.copyOf(values);
		this.labeler = labeler;
		this.onChange = onChange;
		this.value = value;
		this.label(labeler.apply(value));
	}

	public Dropdown<T> columns(int columns) {
		this.columns = columns;
		return this;
	}

	public T value() {
		return this.value;
	}

	public Dropdown<T> value(T value) {
		this.value = value;
		this.label(this.labeler.apply(value));
		return this;
	}

	@Override
	public void press() {
		UiScreen screen = this.screen();
		if (screen == null) {
			return;
		}
		List<MenuOption> options = new ArrayList<>();
		for (T candidate : this.values) {
			options.add(MenuOption.of(this.labeler.apply(candidate), () -> this.choose(candidate)).checked(candidate.equals(this.value)));
		}
		double[] origin = screen.localize(this, 0, 0);
		int screenX = (int) Math.round(this.x - origin[0]);
		int screenY = (int) Math.round(this.bottom() - origin[1]);
		screen.openOverlay(new PopoverMenu(screenX, screenY, false, options).columns(this.columns).minWidth(this.width));
	}

	private void choose(T value) {
		if (!value.equals(this.value)) {
			this.value(value);
			this.onChange.accept(value);
		}
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		super.render(canvas, mouseX, mouseY, delta);
		int arrowWidth = DrawUtil.spriteWidth(PixelSprites.DOWN);
		DrawUtil.sprite(canvas, PixelSprites.DOWN, this.x + this.width - arrowWidth - 4, this.y + (this.height - DrawUtil.spriteHeight(PixelSprites.DOWN)) / 2, this.interactive() ? Theme.LABEL : Theme.DIM);
	}
}
