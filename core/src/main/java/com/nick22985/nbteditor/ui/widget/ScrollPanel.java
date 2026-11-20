package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.WidgetGroup;
import com.nick22985.nbteditor.util.DrawUtil;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class ScrollPanel extends WidgetGroup {
	public static final int PADDING = 4;

	private boolean framed = true;
	private int scroll;
	private int fixedContentHeight = -1;
	private int scrollStep = Theme.ROW * 2;
	private boolean draggingScrollbar;

	public ScrollPanel framed(boolean framed) {
		this.framed = framed;
		this.onLayout();
		return this;
	}

	public boolean framed() {
		return this.framed;
	}

	public int padding() {
		return this.framed ? PADDING : 0;
	}

	public int contentX() {
		return this.x + this.padding();
	}

	public int contentY() {
		return this.y + this.padding();
	}

	public int contentWidth() {
		return this.width - this.padding() * 2 - Theme.SCROLLBAR_WIDTH - 1;
	}

	public ScrollPanel scrollStep(int scrollStep) {
		this.scrollStep = scrollStep;
		return this;
	}

	public ScrollPanel contentHeight(int contentHeight) {
		this.fixedContentHeight = contentHeight;
		this.scroll(this.scroll);
		return this;
	}

	public int contentHeight() {
		if (this.fixedContentHeight >= 0) {
			return this.fixedContentHeight;
		}
		int bottom = this.y;
		for (Widget child : this.children()) {
			if (child.visible()) {
				bottom = Math.max(bottom, child.bottom());
			}
		}
		return bottom - this.y + this.padding();
	}

	public int maxScroll() {
		return Math.max(0, this.contentHeight() - this.height);
	}

	public int scroll() {
		return this.scroll;
	}

	public void scroll(int scroll) {
		this.scroll = DrawUtil.clamp(scroll, 0, this.maxScroll());
	}

	public void scrollToBottom() {
		this.scroll(this.maxScroll());
	}

	public void ensureVisible(int top, int bottom) {
		int viewTop = this.y + this.scroll;
		int viewBottom = viewTop + this.height;
		if (top < viewTop + this.padding()) {
			this.scroll(top - this.y - this.padding());
		} else if (bottom > viewBottom - this.padding()) {
			this.scroll(bottom - this.y - this.height + this.padding());
		}
	}

	public void ensureVisible(Widget widget) {
		this.ensureVisible(widget.y(), widget.bottom());
	}

	@Override
	public int contentOffsetY() {
		return this.scroll;
	}

	@Override
	public boolean clipsChildren() {
		return true;
	}

	@Override
	protected boolean shouldRender(Widget child) {
		return child.bottom() > this.y + this.scroll && child.y() < this.y + this.scroll + this.height;
	}

	@Override
	protected void renderChildren(Canvas canvas, int mouseX, int mouseY, float delta) {
		this.scroll(this.scroll);
		super.renderChildren(canvas, mouseX, mouseY, delta);
	}

	@Override
	protected void renderBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		if (this.framed) {
			DrawUtil.well(canvas, this.x, this.y, this.width, this.height);
		}
	}

	@Override
	protected void renderForeground(Canvas canvas, int mouseX, int mouseY, float delta) {
		DrawUtil.scrollbar(canvas, this.scrollbarX(), this.y + 1, this.height - 2, this.contentHeight(), this.scroll);
	}

	private int scrollbarX() {
		return this.x + this.width - 1 - Theme.SCROLLBAR_WIDTH;
	}

	private boolean overScrollbar(double mouseX, double mouseY) {
		return this.maxScroll() > 0 && this.contains(mouseX, mouseY) && mouseX >= this.scrollbarX();
	}

	@Override
	public Widget widgetAt(double mouseX, double mouseY) {
		if (this.visible() && this.overScrollbar(mouseX, mouseY)) {
			return this;
		}
		return super.widgetAt(mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (input.left() && this.overScrollbar(input.x(), input.y())) {
			this.draggingScrollbar = true;
			this.dragScrollbar(input.y());
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseInput input, double deltaX, double deltaY) {
		if (this.draggingScrollbar) {
			this.dragScrollbar(input.y());
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseInput input) {
		this.draggingScrollbar = false;
		return true;
	}

	private void dragScrollbar(double mouseY) {
		double progress = (mouseY - this.y - 1) / Math.max(1, this.height - 2);
		this.scroll((int) Math.round(progress * this.contentHeight() - this.height / 2.0));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		if (this.maxScroll() <= 0 || deltaY == 0) {
			return false;
		}
		this.scroll(this.scroll - (int) Math.round(deltaY * this.scrollStep));
		return true;
	}
}
