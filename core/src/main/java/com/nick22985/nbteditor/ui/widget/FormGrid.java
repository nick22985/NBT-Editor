package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.WidgetGroup;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class FormGrid extends WidgetGroup {
	public static final int GAP = 4;

	private final List<Text> labels = new ArrayList<>();
	private final List<Widget> widgets = new ArrayList<>();
	private final List<Integer> heights = new ArrayList<>();
	private int labelWidth = 64;

	public FormGrid labelWidth(int labelWidth) {
		this.labelWidth = labelWidth;
		this.onLayout();
		return this;
	}

	public <T extends Widget> T row(Text label, T widget) {
		return this.row(label, widget, Theme.FIELD_HEIGHT);
	}

	public <T extends Widget> T row(Text label, T widget, int height) {
		this.labels.add(label);
		this.widgets.add(this.add(widget));
		this.heights.add(height);
		this.onLayout();
		return widget;
	}

	public int preferredHeight() {
		int height = 0;
		for (int i = 0; i < this.widgets.size(); i++) {
			if (this.widgets.get(i).visible()) {
				height += this.heights.get(i) + GAP;
			}
		}
		return Math.max(0, height - GAP);
	}

	@Override
	protected void onLayout() {
		int top = this.y;
		for (int i = 0; i < this.widgets.size(); i++) {
			Widget widget = this.widgets.get(i);
			if (!widget.visible()) {
				continue;
			}
			int left = this.labels.get(i) == null ? 0 : this.labelWidth;
			widget.bounds(this.x + left, top, this.width - left, this.heights.get(i));
			top += this.heights.get(i) + GAP;
		}
	}

	@Override
	protected void renderBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		for (int i = 0; i < this.widgets.size(); i++) {
			Widget widget = this.widgets.get(i);
			Text label = this.labels.get(i);
			if (label != null && widget.visible()) {
				DrawUtil.textClipped(canvas, label, this.x, widget.y() + Math.min(widget.height(), Theme.FIELD_HEIGHT) / 2 - 4, this.labelWidth - 4, Theme.TEXT);
			}
		}
	}
}
