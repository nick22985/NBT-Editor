package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.function.Supplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class Label extends Widget {
	private Supplier<Text> text;
	private int color = Theme.TEXT;
	private Align align = Align.LEFT;
	private boolean wrap;

	public Label(Text text) {
		this(() -> text);
	}

	public Label(Supplier<Text> text) {
		this.text = text;
		this.height = 9;
	}

	public Label color(int color) {
		this.color = color;
		return this;
	}

	public Label align(Align align) {
		this.align = align;
		return this;
	}

	public Label wrap(boolean wrap) {
		this.wrap = wrap;
		return this;
	}

	public Label text(Text text) {
		this.text = () -> text;
		return this;
	}

	public Text text() {
		return this.text.get();
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		Text value = this.text.get();
		if (value == null) {
			return;
		}
		if (this.wrap) {
			int lineY = this.y;
			for (Text line : canvas.wrap(value, this.width)) {
				if (lineY + 9 > this.y + Math.max(this.height, 9)) {
					break;
				}
				this.renderLine(canvas, line, lineY);
				lineY += Theme.LINE;
			}
			return;
		}
		this.renderLine(canvas, value, this.y + Math.max(0, (this.height - 8) / 2));
	}

	private void renderLine(Canvas canvas, Text line, int lineY) {
		int textWidth = Math.min(canvas.textWidth(line), this.width);
		int textX;
		switch (this.align) {
			case CENTER:
				textX = this.x + (this.width - textWidth) / 2;
				break;
			case RIGHT:
				textX = this.x + this.width - textWidth;
				break;
			default:
				textX = this.x;
				break;
		}
		DrawUtil.textClipped(canvas, line, textX, lineY, this.width, this.color);
	}
}
