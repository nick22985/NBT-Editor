package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.data.ModelEntry;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.ScrollPanel;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ModelGridView extends ScrollPanel {
	public static final int PITCH = Theme.SLOT + 2;
	private static final int HEADER = 13;
	private static final int ROW = 18;
	private static final int CRUMB = 12;
	private static final int BLOCK_MIN = 96;
	private static final int BLOCK_GAP = 8;

	private final Function<ModelEntry, GameItem> items;
	private final BiFunction<ModelGroup, Boolean, Text> titles;
	private final Function<ModelGroup, String> tags;
	private final Function<ModelEntry, List<Text>> tooltips;
	private final BiConsumer<ModelEntry, MouseInput> onClick;
	private final Set<String> folded = new HashSet<>();
	private final List<ModelLine> lines = new ArrayList<>();
	private final List<ModelEntry> order = new ArrayList<>();
	private List<ModelGroup> groups = List.of();
	private boolean headers = true;
	private boolean list;
	private ModelEntry selected;
	private Text crumb;
	private Text empty = Text.EMPTY;

	public ModelGridView(Function<ModelEntry, GameItem> items, BiFunction<ModelGroup, Boolean, Text> titles, Function<ModelGroup, String> tags, Function<ModelEntry, List<Text>> tooltips, BiConsumer<ModelEntry, MouseInput> onClick) {
		this.items = items;
		this.titles = titles;
		this.tags = tags;
		this.tooltips = tooltips;
		this.onClick = onClick;
		this.scrollStep(PITCH);
	}

	public ModelGridView groups(List<ModelGroup> groups, boolean headers) {
		this.groups = groups;
		this.headers = headers;
		this.layout();
		return this;
	}

	public ModelGridView list(boolean list) {
		this.list = list;
		this.layout();
		return this;
	}

	public ModelGridView crumb(Text crumb) {
		this.crumb = crumb;
		this.layout();
		return this;
	}

	public ModelGridView empty(Text empty) {
		this.empty = empty;
		return this;
	}

	public ModelGridView selected(ModelEntry selected) {
		this.selected = selected;
		return this;
	}

	public List<ModelEntry> order() {
		return this.order;
	}

	public int columns() {
		return Math.max(1, (this.contentWidth() + 2) / PITCH);
	}

	@Override
	protected void onLayout() {
		super.onLayout();
		this.layout();
	}

	private void layout() {
		this.lines.clear();
		this.order.clear();
		int left = this.contentX();
		int width = this.contentWidth();
		int top = this.contentY() + (this.crumb == null ? 0 : CRUMB);
		int columns = this.columns();
		int flowTop = -1;
		int flowX = left;
		for (ModelGroup group : this.groups) {
			boolean folded = this.headers && this.folded.contains(group.key());
			List<ModelEntry> entries = group.entries();
			int tiles = entries.size() * PITCH - 2;
			if (this.headers && !this.list && entries.size() * PITCH <= width / 4) {
				int block = Math.max(tiles, BLOCK_MIN);
				if (flowTop < 0 || flowX + block > left + width) {
					if (flowTop >= 0) {
						top = flowTop + HEADER + PITCH + 2;
					}
					flowTop = top;
					flowX = left;
				}
				this.lines.add(new ModelLine(flowX, flowTop, block, HEADER, group, List.of(), true));
				if (!folded) {
					this.lines.add(new ModelLine(flowX, flowTop + HEADER, block, PITCH, null, entries, true));
					this.order.addAll(entries);
				}
				flowX += block + BLOCK_GAP;
				continue;
			}
			if (flowTop >= 0) {
				top = flowTop + HEADER + PITCH + 2;
				flowTop = -1;
			}
			if (this.headers) {
				this.lines.add(new ModelLine(left, top, width, HEADER, group, List.of(), false));
				top += HEADER;
			}
			if (folded) {
				continue;
			}
			if (this.list) {
				for (ModelEntry entry : entries) {
					this.lines.add(new ModelLine(left, top, width, ROW, null, List.of(entry), false));
					top += ROW;
				}
			} else {
				for (int i = 0; i < entries.size(); i += columns) {
					this.lines.add(new ModelLine(left, top, width, PITCH, null, entries.subList(i, Math.min(entries.size(), i + columns)), false));
					top += PITCH;
				}
			}
			this.order.addAll(entries);
			top += 2;
		}
		if (flowTop >= 0) {
			top = flowTop + HEADER + PITCH + 2;
		}
		this.contentHeight(top - this.y + this.padding());
	}

	public boolean fold(String key) {
		if (!this.folded.remove(key)) {
			this.folded.add(key);
		}
		this.layout();
		return true;
	}

	public void reveal(ModelEntry entry) {
		for (ModelLine line : this.lines) {
			if (line.entries().contains(entry)) {
				this.ensureVisible(line.y(), line.bottom());
				return;
			}
		}
	}

	public ModelEntry neighbour(ModelEntry from, int dx, int dy) {
		if (this.order.isEmpty()) {
			return null;
		}
		int index = this.order.indexOf(from);
		if (index < 0) {
			return this.order.get(0);
		}
		if (this.list || dy == 0) {
			int next = index + (dx != 0 ? dx : dy);
			return next >= 0 && next < this.order.size() ? this.order.get(next) : null;
		}
		ModelLine current = this.lines.get(this.lineOf(from));
		int px = current.x() + current.entries().indexOf(from) * PITCH;
		ModelLine best = null;
		int bestY = Integer.MAX_VALUE;
		int bestX = Integer.MAX_VALUE;
		for (ModelLine line : this.lines) {
			if (line.entries().isEmpty() || (dy > 0 ? line.y() <= current.y() : line.y() >= current.y())) {
				continue;
			}
			int distanceY = Math.abs(line.y() - current.y());
			int end = line.x() + (line.entries().size() - 1) * PITCH;
			int distanceX = px < line.x() ? line.x() - px : Math.max(0, px - end);
			if (distanceY < bestY || (distanceY == bestY && distanceX < bestX)) {
				best = line;
				bestY = distanceY;
				bestX = distanceX;
			}
		}
		if (best == null) {
			return null;
		}
		int column = DrawUtil.clamp((px - best.x() + PITCH / 2) / PITCH, 0, best.entries().size() - 1);
		return best.entries().get(column);
	}

	private int lineOf(ModelEntry entry) {
		for (int i = 0; i < this.lines.size(); i++) {
			if (this.lines.get(i).entries().contains(entry)) {
				return i;
			}
		}
		return -1;
	}

	private ModelLine lineAt(double contentX, double contentY) {
		for (ModelLine line : this.lines) {
			if (line.contains(contentX, contentY)) {
				return line;
			}
		}
		return null;
	}

	public ModelEntry entryAt(double mouseX, double mouseY) {
		if (!this.contains(mouseX, mouseY)) {
			return null;
		}
		ModelLine line = this.lineAt(mouseX, mouseY + this.scroll());
		if (line == null || line.header() != null) {
			return null;
		}
		if (this.list) {
			return mouseX < this.contentX() + this.contentWidth() ? line.entries().get(0) : null;
		}
		int column = (int) Math.floor((mouseX - line.x()) / PITCH);
		return column >= 0 && column < line.entries().size() && mouseX - line.x() - column * PITCH < Theme.SLOT ? line.entries().get(column) : null;
	}

	public int[] tilePosition(ModelEntry entry) {
		int lineIndex = this.lineOf(entry);
		if (lineIndex < 0) {
			return null;
		}
		ModelLine line = this.lines.get(lineIndex);
		int column = this.list ? 0 : line.entries().indexOf(entry);
		return new int[] {line.x() + column * PITCH + Theme.SLOT / 2, line.y() - this.scroll() + line.height() / 2};
	}

	@Override
	protected void renderChildren(Canvas canvas, int mouseX, int mouseY, float delta) {
		this.scroll(this.scroll());
		canvas.pushClip(this.x, this.y, this.width, this.height);
		canvas.pushOffset(0, -this.scroll());
		boolean inside = this.contains(mouseX, mouseY) && this.interactive();
		int innerY = mouseY + this.scroll();
		int viewTop = this.y + this.scroll();
		int viewBottom = viewTop + this.height;
		if (this.crumb != null) {
			DrawUtil.textClipped(canvas, this.crumb, this.contentX() + 1, this.contentY() + 1, this.contentWidth() - 2, Theme.FAINT);
		}
		if (this.groups.isEmpty()) {
			DrawUtil.textClipped(canvas, this.empty, this.contentX() + 2, this.contentY() + (this.crumb == null ? 0 : CRUMB) + 3, this.contentWidth() - 4, Theme.FAINT);
		}
		ModelEntry hovered = inside ? this.entryAt(mouseX, mouseY) : null;
		int[] highlight = null;
		for (ModelLine line : this.lines) {
			if (line.bottom() <= viewTop || line.y() >= viewBottom) {
				continue;
			}
			if (line.header() != null) {
				this.drawHeader(canvas, line, inside && line.contains(mouseX, innerY));
			} else if (this.list) {
				this.drawRow(canvas, line, line.entries().get(0) == hovered);
			} else {
				for (int i = 0; i < line.entries().size(); i++) {
					ModelEntry entry = line.entries().get(i);
					int slotX = line.x() + i * PITCH;
					this.drawTile(canvas, entry, slotX, line.y() + 1);
					if (entry == hovered) {
						highlight = new int[] {slotX, line.y() + 1};
					}
				}
			}
		}
		if (highlight != null) {
			canvas.nextLayer();
			DrawUtil.slotHighlight(canvas, highlight[0] + 1, highlight[1] + 1, Theme.SLOT_HIGHLIGHT);
		}
		canvas.popOffset();
		canvas.popClip();
	}

	private void drawHeader(Canvas canvas, ModelLine line, boolean hovered) {
		int left = line.x();
		int right = left + line.width();
		int textY = line.y() + 3;
		boolean open = !this.folded.contains(line.header().key());
		String[] caret = open ? PixelSprites.DOWN : PixelSprites.RIGHT;
		DrawUtil.spriteCentered(canvas, caret, left, line.y(), 7, HEADER - 1, hovered ? Theme.TEXT : Theme.LABEL);
		int textX = left + 9;
		String count = String.valueOf(line.header().entries().size());
		int room = line.compact() ? right - textX - canvas.textWidth(count) - 4 : right - textX - 40;
		textX += DrawUtil.textClipped(canvas, this.titles.apply(line.header(), line.compact()), textX, textY, room, hovered ? Theme.TEXT : Theme.LABEL) + 4;
		DrawUtil.text(canvas, count, textX, textY, Theme.FAINT);
		textX += canvas.textWidth(count) + 4;
		if (line.compact()) {
			return;
		}
		String tag = this.tags.apply(line.header());
		if (tag != null && !tag.isEmpty() && textX + DrawUtil.chipWidth(canvas, tag) < right) {
			textX += DrawUtil.chip(canvas, tag, textX, line.y() + 2, Theme.LABEL) + 4;
		}
		if (textX < right) {
			DrawUtil.rect(canvas, textX, line.y() + HEADER / 2, right - textX, 1, Theme.SECTION_LINE);
		}
	}

	private void drawTile(Canvas canvas, ModelEntry entry, int slotX, int slotY) {
		canvas.slot(slotX, slotY);
		GameItem item = this.items.apply(entry);
		if (item != null && !item.isEmpty()) {
			canvas.pushClip(slotX + 1, slotY + 1, 16, 16);
			canvas.item(item, slotX + 1, slotY + 1, 1.0F, false);
			canvas.popClip();
		}
		if (entry == this.selected) {
			DrawUtil.outline(canvas, slotX, slotY, Theme.SLOT, Theme.SLOT, Theme.YELLOW);
		}
	}

	private void drawRow(Canvas canvas, ModelLine line, boolean hovered) {
		ModelEntry entry = line.entries().get(0);
		int left = this.contentX();
		int width = this.contentWidth();
		if (entry == this.selected) {
			DrawUtil.rect(canvas, left, line.y(), width, ROW, Theme.ROW_SELECTED);
			DrawUtil.rect(canvas, left, line.y(), 1, ROW, Theme.YELLOW);
		} else if (hovered) {
			DrawUtil.rect(canvas, left, line.y(), width, ROW, Theme.ROW_HOVER);
		}
		GameItem item = this.items.apply(entry);
		if (item != null && !item.isEmpty()) {
			canvas.pushClip(left + 2, line.y() + 1, 16, 16);
			canvas.item(item, left + 2, line.y() + 1, 1.0F, false);
			canvas.popClip();
		}
		int right = left + width - 3;
		if (!entry.pack().isEmpty()) {
			int chip = DrawUtil.chipWidth(canvas, entry.pack());
			if (chip < width / 3) {
				right -= chip;
				DrawUtil.chip(canvas, entry.pack(), right, line.y() + 4, Theme.FAINT);
				right -= Theme.GAP;
			}
		}
		int textX = left + 22;
		int textY = line.y() + 5;
		textX += DrawUtil.textClipped(canvas, entry.model().label(), textX, textY, right - textX, Theme.TEXT) + 6;
		if (!entry.model().model().isEmpty()) {
			DrawUtil.textClipped(canvas, entry.model().model(), textX, textY, right - textX, Theme.FAINT);
		}
	}

	@Override
	public List<Text> tooltip(int mouseX, int mouseY) {
		ModelEntry entry = this.entryAt(mouseX, mouseY);
		if (entry != null && !this.list) {
			return this.tooltips.apply(entry);
		}
		ModelLine line = this.contains(mouseX, mouseY) ? this.lineAt(mouseX, mouseY + this.scroll()) : null;
		if (line != null && line.compact() && line.header() != null) {
			return List.of(this.titles.apply(line.header(), false));
		}
		return super.tooltip(mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (super.mouseClicked(input)) {
			return true;
		}
		if (!this.contains(input.x(), input.y())) {
			return false;
		}
		ModelLine line = this.lineAt(input.x(), input.y() + this.scroll());
		if (line != null && line.header() != null) {
			if (input.left()) {
				this.playClick();
				this.fold(line.header().key());
			}
			return true;
		}
		ModelEntry entry = this.entryAt(input.x(), input.y());
		if (entry != null) {
			this.onClick.accept(entry, input);
		}
		return true;
	}
}
