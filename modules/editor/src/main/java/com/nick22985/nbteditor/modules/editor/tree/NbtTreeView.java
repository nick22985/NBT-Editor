package com.nick22985.nbteditor.modules.editor.tree;

import com.nick22985.nbteditor.doc.Selection;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.TagDisplayUtil;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public class NbtTreeView extends Widget {
	private static final int INDENT = 10;
	private static final int TWISTY = 9;

	private final TreeModel model;
	private final Selection selection;
	private final SnbtDialect dialect;
	private Consumer<TreeRow> onToggle = row -> {
	};
	private Consumer<TreeRow> onActivate = row -> {
	};
	private Consumer<Boolean> onToggleAll = expand -> {
	};
	private BiConsumer<TreeRow, MouseInput> onContext = (row, input) -> {
	};
	private Function<NbtPath, List<Text>> invalidTooltip = path -> null;
	private Text emptyText;
	private int scroll;
	private boolean draggingScrollbar;

	public NbtTreeView(TreeModel model, Selection selection, SnbtDialect dialect) {
		this.model = model;
		this.selection = selection;
		this.dialect = dialect;
	}

	public NbtTreeView onToggle(Consumer<TreeRow> onToggle) {
		this.onToggle = onToggle;
		return this;
	}

	public NbtTreeView onActivate(Consumer<TreeRow> onActivate) {
		this.onActivate = onActivate;
		return this;
	}

	public NbtTreeView onToggleAll(Consumer<Boolean> onToggleAll) {
		this.onToggleAll = onToggleAll;
		return this;
	}

	public NbtTreeView onContext(BiConsumer<TreeRow, MouseInput> onContext) {
		this.onContext = onContext;
		return this;
	}

	public NbtTreeView invalidTooltip(Function<NbtPath, List<Text>> invalidTooltip) {
		this.invalidTooltip = invalidTooltip;
		return this;
	}

	public NbtTreeView emptyText(Text emptyText) {
		this.emptyText = emptyText;
		return this;
	}

	public int scroll() {
		return this.scroll;
	}

	public void scroll(int scroll) {
		this.scroll = DrawUtil.clamp(scroll, 0, this.maxScroll());
	}

	private int innerHeight() {
		return this.height - 2;
	}

	private int innerWidth() {
		return this.width - 2 - Theme.SCROLLBAR_WIDTH;
	}

	private int contentHeight() {
		return this.model.rows().size() * Theme.ROW;
	}

	private int maxScroll() {
		return Math.max(0, this.contentHeight() - this.innerHeight());
	}

	public int pageSize() {
		return Math.max(1, this.innerHeight() / Theme.ROW);
	}

	public void refreshed() {
		this.scroll(this.scroll);
	}

	public void scrollTo(NbtPath path) {
		int index = this.model.indexOf(path);
		if (index < 0) {
			return;
		}
		int top = index * Theme.ROW;
		if (top < this.scroll) {
			this.scroll(top);
		} else if (top + Theme.ROW > this.scroll + this.innerHeight()) {
			this.scroll(top + Theme.ROW - this.innerHeight());
		}
	}

	@Override
	public boolean focusable() {
		return true;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		DrawUtil.well(canvas, this.x, this.y, this.width, this.height);
		int innerX = this.x + 1;
		int innerY = this.y + 1;
		int innerWidth = this.innerWidth();
		int innerHeight = this.innerHeight();
		List<TreeRow> rows = this.model.rows();
		if (rows.isEmpty()) {
			if (this.emptyText != null) {
				DrawUtil.textClipped(canvas, this.emptyText, innerX + 4, innerY + 3, innerWidth - 8, Theme.FAINT);
			}
			return;
		}
		boolean inside = DrawUtil.inside(mouseX, mouseY, innerX, innerY, innerWidth, innerHeight);
		NbtPath primary = this.selection.primary();
		canvas.pushClip(innerX, innerY, innerWidth, innerHeight);
		int first = this.scroll / Theme.ROW;
		int last = Math.min(rows.size(), (this.scroll + innerHeight) / Theme.ROW + 1);
		for (int i = first; i < last; i++) {
			TreeRow row = rows.get(i);
			int rowY = innerY + i * Theme.ROW - this.scroll;
			boolean hovered = inside && mouseY >= rowY && mouseY < rowY + Theme.ROW;
			if (this.selection.contains(row.path())) {
				DrawUtil.rect(canvas, innerX, rowY, innerWidth, Theme.ROW, Theme.ROW_SELECTED);
				if (row.path().equals(primary) && this.focused()) {
					DrawUtil.outline(canvas, innerX, rowY, innerWidth, Theme.ROW, Theme.TEXT);
				} else if (row.path().equals(primary)) {
					DrawUtil.outline(canvas, innerX, rowY, innerWidth, Theme.ROW, Theme.LABEL);
				}
			} else if (hovered) {
				DrawUtil.rect(canvas, innerX, rowY, innerWidth, Theme.ROW, Theme.ROW_HOVER);
			}
			this.renderRow(canvas, row, innerX, rowY, innerX + innerWidth - 3);
		}
		canvas.popClip();
		DrawUtil.scrollbar(canvas, this.x + this.width - 1 - Theme.SCROLLBAR_WIDTH, innerY, innerHeight, this.contentHeight(), this.scroll);
	}

	private int renderRow(Canvas canvas, TreeRow row, int rowX, int rowY, int right) {
		int x = rowX + 3 + row.depth() * INDENT;
		if (row.expandable()) {
			String[] arrow = row.expanded() ? PixelSprites.DOWN : PixelSprites.RIGHT;
			int arrowX = x + (7 - DrawUtil.spriteWidth(arrow)) / 2;
			int arrowY = rowY + (Theme.ROW - DrawUtil.spriteHeight(arrow)) / 2;
			DrawUtil.sprite(canvas, arrow, arrowX, arrowY, Theme.LABEL);
		}
		x += TWISTY;
		x += DrawUtil.chip(canvas, TagDisplayUtil.chip(row.tag().type()), x, rowY + 1, TagDisplayUtil.chipColor(row.tag().type())) + 3;
		int keyWidth = canvas.textWidth(row.key());
		if (row.match()) {
			DrawUtil.rect(canvas, x - 1, rowY + 1, keyWidth + 2, 10, Theme.SEARCH_HIGHLIGHT);
		}
		int keyColor = row.invalid() ? Theme.RED : row.listIndex() ? Theme.FAINT : Theme.AQUA;
		x += DrawUtil.textClipped(canvas, row.key(), x, rowY + 2, right - x, keyColor);
		int warnX = -1;
		if (row.invalid()) {
			warnX = x + 2;
			DrawUtil.sprite(canvas, PixelSprites.WARN, warnX, rowY + 3, Theme.RED);
			x += 2 + DrawUtil.spriteWidth(PixelSprites.WARN);
		}
		DrawUtil.text(canvas, ":", x, rowY + 2, Theme.LABEL);
		x += canvas.textWidth(":") + 4;
		if (x < right) {
			DrawUtil.textClipped(canvas, row.value(this.dialect), x, rowY + 2, right - x, Theme.TEXT);
		}
		return warnX;
	}

	@Override
	public List<Text> tooltip(int mouseX, int mouseY) {
		TreeRow row = this.rowAt(mouseY);
		if (row != null && row.invalid() && mouseX < this.x + this.innerWidth()) {
			return this.invalidTooltip.apply(row.path());
		}
		return super.tooltip(mouseX, mouseY);
	}

	private TreeRow rowAt(double mouseY) {
		if (mouseY < this.y + 1 || mouseY >= this.y + 1 + this.innerHeight()) {
			return null;
		}
		int index = ((int) mouseY - this.y - 1 + this.scroll) / Theme.ROW;
		List<TreeRow> rows = this.model.rows();
		return index >= 0 && index < rows.size() ? rows.get(index) : null;
	}

	@Override
	public boolean mouseClicked(MouseInput input) {
		if (!this.contains(input.x(), input.y())) {
			return false;
		}
		int scrollbarX = this.x + this.width - 1 - Theme.SCROLLBAR_WIDTH;
		if (input.left() && input.x() >= scrollbarX && this.maxScroll() > 0) {
			this.draggingScrollbar = true;
			this.dragScrollbar(input.y());
			return true;
		}
		TreeRow row = this.rowAt(input.y());
		if (input.right()) {
			if (row != null && !this.selection.contains(row.path())) {
				this.selection.select(row.path());
			}
			this.onContext.accept(row, input);
			return true;
		}
		if (!input.left()) {
			return true;
		}
		if (row == null) {
			if (!input.modifiers().primary() && !input.modifiers().shift()) {
				this.selection.clear();
			}
			return true;
		}
		int arrowX = this.x + 1 + 3 + row.depth() * INDENT;
		if (row.expandable() && input.x() >= arrowX - 2 && input.x() < arrowX + TWISTY) {
			this.onToggle.accept(row);
		} else if (input.modifiers().primary()) {
			this.selection.toggle(row.path());
		} else if (input.modifiers().shift()) {
			this.selection.extendTo(row.path(), this.model.order());
		} else if (input.doubleClick() && this.selection.primary() != null && this.selection.primary().equals(row.path())) {
			if (row.expandable()) {
				this.onToggle.accept(row);
			} else {
				this.onActivate.accept(row);
			}
		} else {
			this.selection.select(row.path());
		}
		return true;
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
		boolean was = this.draggingScrollbar;
		this.draggingScrollbar = false;
		return was;
	}

	private void dragScrollbar(double mouseY) {
		int innerHeight = this.innerHeight();
		double progress = (mouseY - this.y - 1) / innerHeight;
		this.scroll((int) Math.round(progress * this.contentHeight() - innerHeight / 2.0));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		if (!this.contains(mouseX, mouseY)) {
			return false;
		}
		this.scroll(this.scroll - (int) Math.round(deltaY * Theme.ROW * 2));
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		List<TreeRow> rows = this.model.rows();
		if (rows.isEmpty()) {
			return false;
		}
		boolean shift = input.modifiers().shift();
		if (input.shortcut(Key.A)) {
			this.selection.selectAll(this.model.order());
			return true;
		}
		if (input.modifiers().primary() || input.modifiers().alt()) {
			return false;
		}
		int current = this.selection.primary() == null ? -1 : this.model.indexOf(this.selection.primary());
		switch (input.key()) {
			case Key.UP:
				this.moveTo(current < 0 ? rows.size() - 1 : current - 1, shift);
				return true;
			case Key.DOWN:
				this.moveTo(current < 0 ? 0 : current + 1, shift);
				return true;
			case Key.PAGE_UP:
				this.moveTo(Math.max(0, current - this.pageSize()), shift);
				return true;
			case Key.PAGE_DOWN:
				this.moveTo(current < 0 ? this.pageSize() : current + this.pageSize(), shift);
				return true;
			case Key.HOME:
				this.moveTo(0, shift);
				return true;
			case Key.END:
				this.moveTo(rows.size() - 1, shift);
				return true;
			case Key.LEFT:
				if (shift) {
					this.onToggleAll.accept(false);
				} else if (current >= 0) {
					TreeRow row = rows.get(current);
					if (row.expandable() && row.expanded()) {
						this.onToggle.accept(row);
					} else {
						int parent = this.model.indexOf(row.path().parent());
						if (parent >= 0) {
							this.moveTo(parent, false);
						}
					}
				}
				return true;
			case Key.RIGHT:
				if (shift) {
					this.onToggleAll.accept(true);
				} else if (current >= 0) {
					TreeRow row = rows.get(current);
					if (row.expandable() && !row.expanded()) {
						this.onToggle.accept(row);
					} else if (row.expandable()) {
						this.moveTo(current + 1, false);
					}
				}
				return true;
			case Key.ENTER:
			case Key.KP_ENTER:
				if (current >= 0) {
					this.onActivate.accept(rows.get(current));
					return true;
				}
				return false;
			default:
				return false;
		}
	}

	private void moveTo(int index, boolean extend) {
		List<TreeRow> rows = this.model.rows();
		if (rows.isEmpty()) {
			return;
		}
		NbtPath path = rows.get(DrawUtil.clamp(index, 0, rows.size() - 1)).path();
		if (extend && this.selection.anchor() != null) {
			this.selection.extendTo(path, this.model.order());
		} else {
			this.selection.select(path);
		}
		this.scrollTo(path);
	}
}
