package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.TextMetrics;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class Breadcrumbs extends Widget {
	private static final int SEPARATOR_GAP = 3;
	private static final Text ELLIPSIS = Text.literal("…");

	private final List<Text> crumbs = new ArrayList<>();
	private IntConsumer onSelect = index -> {
	};

	public Breadcrumbs() {
		this.height = Theme.ROW;
	}

	public Breadcrumbs crumbs(List<Text> crumbs) {
		this.crumbs.clear();
		this.crumbs.addAll(crumbs);
		return this;
	}

	public Breadcrumbs onSelect(IntConsumer onSelect) {
		this.onSelect = onSelect;
		return this;
	}

	private int separatorWidth() {
		return DrawUtil.spriteWidth(PixelSprites.RIGHT) + SEPARATOR_GAP * 2;
	}

	private int firstVisible(TextMetrics metrics) {
		int total = 0;
		for (int i = this.crumbs.size() - 1; i >= 0; i--) {
			total += metrics.textWidth(this.crumbs.get(i)) + (i > 0 ? this.separatorWidth() : 0);
			if (total > this.width && i < this.crumbs.size() - 1) {
				return i + 1;
			}
		}
		return 0;
	}

	private int[] positions(TextMetrics metrics) {
		int first = this.firstVisible(metrics);
		int[] positions = new int[this.crumbs.size() + 1];
		int offset = this.x;
		if (first > 0) {
			offset += metrics.textWidth(ELLIPSIS) + this.separatorWidth();
		}
		for (int i = 0; i < this.crumbs.size(); i++) {
			if (i < first) {
				positions[i] = Integer.MIN_VALUE;
				continue;
			}
			positions[i] = offset;
			offset += metrics.textWidth(this.crumbs.get(i)) + this.separatorWidth();
		}
		positions[this.crumbs.size()] = first;
		return positions;
	}

	private int indexAt(TextMetrics metrics, double mouseX, double mouseY) {
		if (!this.contains(mouseX, mouseY)) {
			return -1;
		}
		int[] positions = this.positions(metrics);
		int first = positions[this.crumbs.size()];
		if (first > 0 && mouseX < this.x + metrics.textWidth(ELLIPSIS)) {
			return first - 1;
		}
		for (int i = first; i < this.crumbs.size(); i++) {
			int width = metrics.textWidth(this.crumbs.get(i));
			if (mouseX >= positions[i] && mouseX < positions[i] + width) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		int[] positions = this.positions(canvas);
		int first = positions[this.crumbs.size()];
		int hovered = this.indexAt(canvas, mouseX, mouseY);
		int textY = this.y + (this.height - 8) / 2;
		canvas.pushClip(this.x, this.y, this.width, this.height);
		if (first > 0) {
			DrawUtil.text(canvas, ELLIPSIS, this.x, textY, hovered == first - 1 ? Theme.YELLOW : Theme.FAINT);
			DrawUtil.sprite(canvas, PixelSprites.RIGHT, this.x + canvas.textWidth(ELLIPSIS) + SEPARATOR_GAP, textY + 1, Theme.DIM);
		}
		for (int i = first; i < this.crumbs.size(); i++) {
			boolean last = i == this.crumbs.size() - 1;
			int color = last ? Theme.TEXT : hovered == i ? Theme.YELLOW : Theme.LABEL;
			DrawUtil.text(canvas, this.crumbs.get(i), positions[i], textY, color);
			if (!last) {
				int separatorX = positions[i] + canvas.textWidth(this.crumbs.get(i)) + SEPARATOR_GAP;
				DrawUtil.sprite(canvas, PixelSprites.RIGHT, separatorX, textY + 1, Theme.DIM);
			}
		}
		canvas.popClip();
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		UiHost host = this.host();
		if (!input.left() || host == null) {
			return false;
		}
		int index = this.indexAt(host, input.x(), input.y());
		if (index >= 0 && index < this.crumbs.size() - 1) {
			this.playClick();
			this.onSelect.accept(index);
			return true;
		}
		return false;
	}
}
