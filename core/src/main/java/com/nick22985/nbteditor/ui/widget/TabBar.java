package com.nick22985.nbteditor.ui.widget;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.TextMetrics;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiHost;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class TabBar extends Widget {
	public static final int HEIGHT = 15;
	public static final String MORE = "more";
	private static final int PADDING = 6;
	private static final int SPACING = 2;

	private final List<TabSpec> tabs;
	private final Consumer<TabSpec> onSelect;
	private String selected;
	private Text moreLabel;
	private Consumer<List<TabSpec>> onMore;

	public TabBar(List<TabSpec> tabs, String selected, Consumer<TabSpec> onSelect) {
		this.tabs = List.copyOf(tabs);
		this.selected = selected;
		this.onSelect = onSelect;
		this.height = HEIGHT;
	}

	public TabBar more(Text label, Consumer<List<TabSpec>> onMore) {
		this.moreLabel = label;
		this.onMore = onMore;
		return this;
	}

	public String selected() {
		return this.selected;
	}

	public TabBar selected(String selected) {
		this.selected = selected;
		return this;
	}

	private int moreWidth(TextMetrics metrics) {
		return this.moreLabel == null ? 0 : metrics.textWidth(this.moreLabel) + PADDING * 2;
	}

	private int tabWidth(TextMetrics metrics, TabSpec tab) {
		return metrics.textWidth(tab.label()) + PADDING * 2;
	}

	private int visibleCount(TextMetrics metrics) {
		int limit = this.x + this.width - this.moreWidth(metrics) - SPACING;
		int offset = this.x;
		int count = 0;
		for (TabSpec tab : this.tabs) {
			int tabWidth = this.tabWidth(metrics, tab);
			if (offset + tabWidth > limit) {
				break;
			}
			offset += tabWidth + SPACING;
			count++;
		}
		return count;
	}

	public List<TabSpec> overflow(TextMetrics metrics) {
		return this.tabs.subList(this.visibleCount(metrics), this.tabs.size());
	}

	private int tabAt(TextMetrics metrics, double mouseX, double mouseY) {
		if (mouseY < this.y || mouseY >= this.y + this.height) {
			return -2;
		}
		int count = this.visibleCount(metrics);
		int offset = this.x;
		for (int i = 0; i < count; i++) {
			int tabWidth = this.tabWidth(metrics, this.tabs.get(i));
			if (mouseX >= offset && mouseX < offset + tabWidth) {
				return i;
			}
			offset += tabWidth + SPACING;
		}
		int moreWidth = this.moreWidth(metrics);
		if (moreWidth > 0 && mouseX >= this.x + this.width - moreWidth && mouseX < this.x + this.width) {
			return -1;
		}
		return -2;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		int count = this.visibleCount(canvas);
		int hovered = this.tabAt(canvas, mouseX, mouseY);
		int offset = this.x;
		boolean selectedVisible = false;
		for (int i = 0; i < count; i++) {
			TabSpec tab = this.tabs.get(i);
			int tabWidth = this.tabWidth(canvas, tab);
			boolean selected = tab.id().equals(this.selected);
			selectedVisible |= selected;
			int color = !tab.available() ? Theme.DIM : selected || hovered == i ? Theme.TEXT : Theme.LABEL;
			DrawUtil.text(canvas, tab.label(), offset + PADDING, this.y + 2, color);
			if (selected) {
				this.underline(canvas, offset, tabWidth);
			}
			offset += tabWidth + SPACING;
		}
		if (this.moreLabel != null) {
			int moreWidth = this.moreWidth(canvas);
			int moreX = this.x + this.width - moreWidth;
			boolean selected = MORE.equals(this.selected) || !selectedVisible;
			DrawUtil.text(canvas, this.moreLabel, moreX + PADDING, this.y + 2, selected || hovered == -1 ? Theme.TEXT : Theme.LABEL);
			if (selected) {
				this.underline(canvas, moreX, moreWidth);
			}
		}
	}

	private void underline(Canvas canvas, int tabX, int tabWidth) {
		DrawUtil.rect(canvas, tabX + 4, this.y + this.height - 1, tabWidth - 6, 1, Theme.SCROLLBAR_DARK);
		DrawUtil.rect(canvas, tabX + 3, this.y + this.height - 2, tabWidth - 6, 1, Theme.TEXT);
	}

	@Override
	public List<Text> tooltip(int mouseX, int mouseY) {
		UiHost host = this.host();
		if (host != null) {
			int index = this.tabAt(host, mouseX, mouseY);
			if (index >= 0) {
				TabSpec tab = this.tabs.get(index);
				if (!tab.available() && tab.unavailableReason() != null) {
					return List.of(tab.unavailableReason());
				}
			}
		}
		return super.tooltip(mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		UiHost host = this.host();
		if (!input.left() || host == null) {
			return false;
		}
		int index = this.tabAt(host, input.x(), input.y());
		if (index >= 0) {
			TabSpec tab = this.tabs.get(index);
			if (tab.available() && !tab.id().equals(this.selected)) {
				this.playClick();
				this.selected = tab.id();
				this.onSelect.accept(tab);
			}
			return true;
		}
		if (index == -1 && this.onMore != null) {
			this.playClick();
			this.onMore.accept(this.overflow(host));
			return true;
		}
		return false;
	}
}
