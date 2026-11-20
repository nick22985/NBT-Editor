package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.TextMetrics;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.TextStyleUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class Segmented extends Widget {
	private static final int PADDING = 5;

	private final List<Text> labels;
	private final IntConsumer onSelect;
	private final Set<Integer> disabled = new HashSet<>();
	private int selected;

	public Segmented(List<Text> labels, int selected, IntConsumer onSelect) {
		this.labels = List.copyOf(labels);
		this.selected = selected;
		this.onSelect = onSelect;
		this.height = Theme.BUTTON_HEIGHT;
	}

	public int preferredWidth(TextMetrics metrics) {
		int total = 1;
		for (Text label : this.labels) {
			total += metrics.textWidth(label) + PADDING * 2 - 1;
		}
		return total;
	}

	public int selected() {
		return this.selected;
	}

	public Segmented selected(int selected) {
		this.selected = selected;
		return this;
	}

	public Segmented segmentEnabled(int index, boolean enabled) {
		if (enabled) {
			this.disabled.remove(index);
		} else {
			this.disabled.add(index);
		}
		return this;
	}

	private int[] offsets(TextMetrics metrics) {
		int[] offsets = new int[this.labels.size() + 1];
		int offset = 0;
		for (int i = 0; i < this.labels.size(); i++) {
			offsets[i] = offset;
			offset += metrics.textWidth(this.labels.get(i)) + PADDING * 2 - 1;
		}
		offsets[this.labels.size()] = offset + 1;
		return offsets;
	}

	private int indexAt(double mouseX, TextMetrics metrics) {
		int[] offsets = this.offsets(metrics);
		for (int i = 0; i < this.labels.size(); i++) {
			if (mouseX >= this.x + offsets[i] && mouseX < this.x + offsets[i + 1]) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean focusable() {
		return true;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		int[] offsets = this.offsets(canvas);
		int hovered = this.contains(mouseX, mouseY) ? this.indexAt(mouseX, canvas) : -1;
		for (int i = 0; i < this.labels.size(); i++) {
			int segmentX = this.x + offsets[i];
			int segmentWidth = offsets[i + 1] - offsets[i] + (i == this.labels.size() - 1 ? 0 : 1);
			boolean active = this.interactive() && !this.disabled.contains(i);
			canvas.button(segmentX, this.y, segmentWidth, this.height, active && (hovered == i || this.focused() && this.selected == i), active);
			Text label = this.labels.get(i);
			int color = !active ? Theme.FAINT : i == this.selected ? Theme.YELLOW : Theme.TEXT;
			int textX = segmentX + (segmentWidth - canvas.textWidth(label)) / 2;
			canvas.text(TextStyleUtil.colored(label, color), textX, this.y + (this.height - 8) / 2, color, true);
		}
	}

	private void select(int index) {
		if (index < 0 || index >= this.labels.size() || index == this.selected || this.disabled.contains(index)) {
			return;
		}
		this.selected = index;
		this.onSelect.accept(index);
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		UiHost host = this.host();
		if (!input.left() || host == null || !this.contains(input.x(), input.y())) {
			return false;
		}
		int index = this.indexAt(input.x(), host);
		if (index >= 0 && !this.disabled.contains(index)) {
			this.playClick();
			this.select(index);
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (!this.focused()) {
			return false;
		}
		if (input.plain(Key.LEFT)) {
			this.select(this.selected - 1);
			return true;
		}
		if (input.plain(Key.RIGHT)) {
			this.select(this.selected + 1);
			return true;
		}
		return false;
	}
}
