package com.nick22985.nbteditor.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class WidgetGroup extends Widget {
	private final List<Widget> children = new ArrayList<>();

	public <T extends Widget> T add(T widget) {
		if (widget.parent() != null) {
			widget.parent().remove(widget);
		}
		this.children.add(widget);
		widget.attachTo(this);
		return widget;
	}

	public void remove(Widget widget) {
		if (this.children.remove(widget)) {
			UiScreen screen = this.screen();
			if (screen != null) {
				screen.forget(widget);
			}
			widget.attachTo(null);
		}
	}

	public void clear() {
		UiScreen screen = this.screen();
		for (Widget child : this.children) {
			if (screen != null) {
				screen.forget(child);
			}
			child.attachTo(null);
		}
		this.children.clear();
	}

	public List<Widget> children() {
		return Collections.unmodifiableList(this.children);
	}

	public int contentOffsetX() {
		return 0;
	}

	public int contentOffsetY() {
		return 0;
	}

	public boolean clipsChildren() {
		return false;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		this.renderBackground(canvas, mouseX, mouseY, delta);
		this.renderChildren(canvas, mouseX, mouseY, delta);
		this.renderForeground(canvas, mouseX, mouseY, delta);
	}

	protected void renderBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
	}

	protected void renderForeground(Canvas canvas, int mouseX, int mouseY, float delta) {
	}

	protected void renderChildren(Canvas canvas, int mouseX, int mouseY, float delta) {
		int dx = this.contentOffsetX();
		int dy = this.contentOffsetY();
		boolean clip = this.clipsChildren();
		if (clip) {
			canvas.pushClip(this.x, this.y, this.width, this.height);
		}
		boolean offset = dx != 0 || dy != 0;
		if (offset) {
			canvas.pushOffset(-dx, -dy);
		}
		int innerX = mouseX + dx;
		int innerY = mouseY + dy;
		if (clip && !this.contains(mouseX, mouseY)) {
			innerX = Integer.MIN_VALUE / 2;
			innerY = Integer.MIN_VALUE / 2;
		}
		for (Widget child : List.copyOf(this.children)) {
			if (child.visible() && this.shouldRender(child)) {
				child.render(canvas, innerX, innerY, delta);
			}
		}
		if (offset) {
			canvas.popOffset();
		}
		if (clip) {
			canvas.popClip();
		}
	}

	protected boolean shouldRender(Widget child) {
		return true;
	}

	@Override
	public Widget widgetAt(double mouseX, double mouseY) {
		if (!this.visible()) {
			return null;
		}
		boolean inside = this.contains(mouseX, mouseY);
		if (this.clipsChildren() && !inside) {
			return null;
		}
		double innerX = mouseX + this.contentOffsetX();
		double innerY = mouseY + this.contentOffsetY();
		for (int i = this.children.size() - 1; i >= 0; i--) {
			Widget found = this.children.get(i).widgetAt(innerX, innerY);
			if (found != null) {
				return found;
			}
		}
		return inside ? this : null;
	}

	@Override
	public void tick() {
		for (Widget child : List.copyOf(this.children)) {
			child.tick();
		}
	}

	public void collectFocusable(List<Widget> out) {
		for (Widget child : this.children) {
			if (!child.visible() || !child.enabled()) {
				continue;
			}
			if (child.focusable()) {
				out.add(child);
			}
			if (child instanceof WidgetGroup group) {
				group.collectFocusable(out);
			}
		}
	}

	public boolean isAncestorOf(Widget widget) {
		for (Widget current = widget; current != null; current = current.parent()) {
			if (current == this) {
				return true;
			}
		}
		return false;
	}
}
