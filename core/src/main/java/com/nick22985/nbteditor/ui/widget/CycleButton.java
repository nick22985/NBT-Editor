package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.MouseInput;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class CycleButton<T> extends Button {
	private final List<T> values;
	private final Function<T, Text> labeler;
	private final Text prefix;
	private Consumer<T> onChange;
	private int index;

	public CycleButton(Text prefix, List<T> values, T value, Function<T, Text> labeler, Consumer<T> onChange) {
		super(Text.EMPTY, button -> {
		});
		this.prefix = prefix;
		this.values = List.copyOf(values);
		this.labeler = labeler;
		this.onChange = onChange;
		this.index = Math.max(0, this.values.indexOf(value));
		this.refresh();
	}

	public T value() {
		return this.values.get(this.index);
	}

	public CycleButton<T> value(T value) {
		int found = this.values.indexOf(value);
		if (found >= 0) {
			this.index = found;
			this.refresh();
		}
		return this;
	}

	private void refresh() {
		Text valueLabel = this.labeler.apply(this.value());
		this.label(this.prefix == null ? valueLabel : this.prefix.append(": ").append(valueLabel));
	}

	private void cycle(int step) {
		this.index = Math.floorMod(this.index + step, this.values.size());
		this.refresh();
		this.onChange.accept(this.value());
	}

	@Override
	public void press() {
		this.cycle(this.host() != null && this.host().modifiers().shift() ? -1 : 1);
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (input.right() && this.interactive() && this.contains(input.x(), input.y())) {
			this.playClick();
			this.cycle(-1);
			return true;
		}
		return super.mouseClicked(input);
	}
}
