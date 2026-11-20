package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Rect;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.WidgetGroup;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class Row extends WidgetGroup {
	private final Map<Widget, Rect> placements = new IdentityHashMap<>();
	private final List<BiConsumer<Canvas, Row>> backgrounds = new ArrayList<>();
	private final List<BiConsumer<Canvas, Row>> decorations = new ArrayList<>();
	private boolean hoverable;
	private boolean selected;
	private Consumer<MouseInput> onClick;

	public Row(int height) {
		this.height = height;
	}

	public <T extends Widget> T place(T widget, int dx, int dy, int width, int height) {
		this.add(widget);
		this.placements.put(widget, new Rect(dx, dy, width, height));
		this.position(widget);
		return widget;
	}

	public <T extends Widget> T placeRight(T widget, int rightInset, int dy, int width, int height) {
		return this.place(widget, -(rightInset + width), dy, width, height);
	}

	public Row background(BiConsumer<Canvas, Row> background) {
		this.backgrounds.add(background);
		return this;
	}

	public Row decorate(BiConsumer<Canvas, Row> decoration) {
		this.decorations.add(decoration);
		return this;
	}

	public Row hoverable(boolean hoverable) {
		this.hoverable = hoverable;
		return this;
	}

	public Row selected(boolean selected) {
		this.selected = selected;
		return this;
	}

	public boolean selected() {
		return this.selected;
	}

	public Row onClick(Consumer<MouseInput> onClick) {
		this.onClick = onClick;
		this.hoverable = true;
		return this;
	}

	@Override
	public void remove(Widget widget) {
		super.remove(widget);
		this.placements.remove(widget);
	}

	@Override
	protected void onLayout() {
		for (Widget child : this.children()) {
			this.position(child);
		}
	}

	private void position(Widget widget) {
		Rect rect = this.placements.get(widget);
		if (rect == null) {
			return;
		}
		int childX = rect.x() < 0 ? this.x + this.width + rect.x() : this.x + rect.x();
		int childWidth = rect.width() <= 0 ? this.x + this.width + rect.width() - childX : rect.width();
		widget.bounds(childX, this.y + rect.y(), childWidth, rect.height());
	}

	@Override
	protected void renderBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		if (this.selected) {
			DrawUtil.rect(canvas, this.x, this.y, this.width, this.height, Theme.ROW_SELECTED);
		} else if (this.hoverable && this.interactive() && this.contains(mouseX, mouseY)) {
			DrawUtil.rect(canvas, this.x, this.y, this.width, this.height, Theme.ROW_HOVER);
		}
		for (BiConsumer<Canvas, Row> background : this.backgrounds) {
			background.accept(canvas, this);
		}
	}

	@Override
	protected void renderForeground(Canvas canvas, int mouseX, int mouseY, float delta) {
		for (BiConsumer<Canvas, Row> decoration : this.decorations) {
			decoration.accept(canvas, this);
		}
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (this.onClick != null && this.contains(input.x(), input.y())) {
			this.onClick.accept(input);
			return true;
		}
		return false;
	}
}
